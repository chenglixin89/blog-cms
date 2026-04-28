# blog-backend

Spring Boot 3 后端最小可用登录版本，已提供：

- POST `/api/admin/login`
- 账号密码校验
- JWT 生成返回
- 统一响应体和全局异常处理

## 默认账号

- username: `admin`
- password: `123456`

配置在 `src/main/resources/application.yml`。

## 运行

```bash
mvn spring-boot:run
```

启动后接口地址：`http://localhost:8080/api/admin/login`
