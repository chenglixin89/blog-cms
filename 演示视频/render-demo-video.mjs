import { spawn } from "node:child_process";
import { randomBytes } from "node:crypto";
import { existsSync, mkdirSync, statSync } from "node:fs";
import { connect } from "node:net";
import { resolve } from "node:path";

const edgePath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const root = resolve(".");
const videoDir = resolve(root, "演示视频");
const downloadDir = resolve(videoDir, "output");
const htmlPath = resolve(videoDir, "demo-recorder.html");
const userDataDir = resolve(videoDir, ".edge-profile");
const outputPath = resolve(downloadDir, "个人博客CMS演示视频.mp4");
const port = 9333 + Math.floor(Math.random() * 400);

mkdirSync(downloadDir, { recursive: true });
mkdirSync(userDataDir, { recursive: true });

if (!existsSync(edgePath)) {
  throw new Error(`未找到 Edge：${edgePath}`);
}

const edge = spawn(edgePath, [
  "--headless=new",
  "--disable-gpu",
  "--no-sandbox",
  "--autoplay-policy=no-user-gesture-required",
  "--disable-background-timer-throttling",
  "--disable-renderer-backgrounding",
  "--disable-backgrounding-occluded-windows",
  `--remote-debugging-port=${port}`,
  `--user-data-dir=${userDataDir}`,
  `file:///${htmlPath.replace(/\\/g, "/")}`
], {
  stdio: ["ignore", "pipe", "pipe"]
});

edge.stderr.on("data", chunk => {
  const text = chunk.toString();
  if (!/ERROR|FATAL/i.test(text)) return;
  process.stderr.write(text);
});

const sleep = ms => new Promise(resolveSleep => setTimeout(resolveSleep, ms));

async function getJson(url, attempts = 60) {
  for (let index = 0; index < attempts; index += 1) {
    try {
      const response = await fetch(url);
      if (response.ok) return response.json();
    } catch {
      await sleep(500);
    }
  }
  throw new Error(`无法连接 Edge 调试端口：${url}`);
}

class SimpleWebSocket {
  constructor(url) {
    this.listeners = new Map();
    this.buffer = Buffer.alloc(0);
    this.handshakeDone = false;
    const parsed = new URL(url);
    this.socket = connect(Number(parsed.port), parsed.hostname);
    this.ready = new Promise((resolveReady, rejectReady) => {
      this.socket.once("connect", () => {
        const key = randomBytes(16).toString("base64");
        this.socket.write([
          `GET ${parsed.pathname}${parsed.search} HTTP/1.1`,
          `Host: ${parsed.host}`,
          "Upgrade: websocket",
          "Connection: Upgrade",
          `Sec-WebSocket-Key: ${key}`,
          "Sec-WebSocket-Version: 13",
          "",
          ""
        ].join("\r\n"));
      });
      this.socket.once("error", rejectReady);
      this.once("open", resolveReady);
    });
    this.socket.on("data", chunk => this.handleData(chunk));
  }

  once(type, callback) {
    const wrapped = (...args) => {
      this.off(type, wrapped);
      callback(...args);
    };
    this.on(type, wrapped);
  }

  on(type, callback) {
    const items = this.listeners.get(type) ?? [];
    items.push(callback);
    this.listeners.set(type, items);
  }

  off(type, callback) {
    this.listeners.set(type, (this.listeners.get(type) ?? []).filter(item => item !== callback));
  }

  emit(type, ...args) {
    (this.listeners.get(type) ?? []).forEach(callback => callback(...args));
  }

  handleData(chunk) {
    this.buffer = Buffer.concat([this.buffer, chunk]);
    if (!this.handshakeDone) {
      const end = this.buffer.indexOf("\r\n\r\n");
      if (end < 0) return;
      this.handshakeDone = true;
      this.buffer = this.buffer.subarray(end + 4);
      this.emit("open");
    }

    while (this.buffer.length >= 2) {
      const first = this.buffer[0];
      const second = this.buffer[1];
      const opcode = first & 0x0f;
      let offset = 2;
      let length = second & 0x7f;
      if (length === 126) {
        if (this.buffer.length < offset + 2) return;
        length = this.buffer.readUInt16BE(offset);
        offset += 2;
      } else if (length === 127) {
        if (this.buffer.length < offset + 8) return;
        length = Number(this.buffer.readBigUInt64BE(offset));
        offset += 8;
      }
      const masked = Boolean(second & 0x80);
      const maskLength = masked ? 4 : 0;
      if (this.buffer.length < offset + maskLength + length) return;
      let payload = this.buffer.subarray(offset + maskLength, offset + maskLength + length);
      if (masked) {
        const mask = this.buffer.subarray(offset, offset + 4);
        payload = Buffer.from(payload.map((value, index) => value ^ mask[index % 4]));
      }
      this.buffer = this.buffer.subarray(offset + maskLength + length);
      if (opcode === 1) this.emit("message", payload.toString("utf8"));
      if (opcode === 8) this.close();
    }
  }

  send(text) {
    const payload = Buffer.from(text);
    const mask = randomBytes(4);
    const header = [];
    header.push(0x81);
    if (payload.length < 126) {
      header.push(0x80 | payload.length);
    } else if (payload.length < 65536) {
      header.push(0x80 | 126, (payload.length >> 8) & 0xff, payload.length & 0xff);
    } else {
      header.push(0x80 | 127, 0, 0, 0, 0);
      const lengthBuffer = Buffer.alloc(4);
      lengthBuffer.writeUInt32BE(payload.length);
      header.push(...lengthBuffer);
    }
    const masked = Buffer.from(payload.map((value, index) => value ^ mask[index % 4]));
    this.socket.write(Buffer.concat([Buffer.from(header), mask, masked]));
  }

  close() {
    this.socket.end();
  }
}

class Cdp {
  constructor(url) {
    this.nextId = 1;
    this.pending = new Map();
    this.socket = new SimpleWebSocket(url);
    this.ready = this.socket.ready;
    this.socket.on("message", message => {
      const data = JSON.parse(message);
      if (data.id && this.pending.has(data.id)) {
        const { resolve: resolvePending, reject } = this.pending.get(data.id);
        this.pending.delete(data.id);
        if (data.error) reject(new Error(data.error.message));
        else resolvePending(data.result);
      }
    });
  }

  async send(method, params = {}) {
    await this.ready;
    const id = this.nextId;
    this.nextId += 1;
    const promise = new Promise((resolvePromise, rejectPromise) => {
      this.pending.set(id, { resolve: resolvePromise, reject: rejectPromise });
    });
    this.socket.send(JSON.stringify({ id, method, params }));
    return promise;
  }

  close() {
    this.socket.close();
  }
}

try {
  const pages = await getJson(`http://127.0.0.1:${port}/json/list`);
  const page = pages.find(item => item.type === "page") ?? pages[0];
  if (!page?.webSocketDebuggerUrl) throw new Error("未找到页面调试地址");

  const cdp = new Cdp(page.webSocketDebuggerUrl);
  await cdp.send("Page.enable");
  await cdp.send("Runtime.enable");
  await cdp.send("Page.setDownloadBehavior", { behavior: "allow", downloadPath: downloadDir });
  await cdp.send("Runtime.evaluate", {
    expression: "window.renderDone",
    awaitPromise: true,
    returnByValue: true,
    timeout: 180000
  });

  for (let index = 0; index < 80; index += 1) {
    if (existsSync(outputPath) && statSync(outputPath).size > 1024 * 1024) break;
    await sleep(500);
  }

  if (!existsSync(outputPath)) {
    throw new Error("录制结束，但没有找到下载的视频文件");
  }

  const size = statSync(outputPath).size;
  console.log(JSON.stringify({ outputPath, size }, null, 2));
  cdp.close();
} finally {
  edge.kill();
}
