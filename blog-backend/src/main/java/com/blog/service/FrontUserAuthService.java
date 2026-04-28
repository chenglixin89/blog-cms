package com.blog.service;

import com.blog.dto.ArticleResponse;
import com.blog.dto.CommentResponse;
import com.blog.dto.FrontUserPasswordUpdateRequest;
import com.blog.dto.FrontUserProfileResponse;
import com.blog.dto.FrontUserProfileUpdateRequest;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.RegisterRequest;
import com.blog.dto.UserActivityStatsResponse;
import com.blog.dto.UserCenterResponse;
import com.blog.entity.BlogUser;
import com.blog.mapper.BlogUserMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.utils.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FrontUserAuthService {

    private static final String ROLE_USER = "USER";

    private final BlogUserMapper blogUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final ArticleService articleService;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserLikeMapper userLikeMapper;
    private final CommentMapper commentMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public FrontUserAuthService(
        BlogUserMapper blogUserMapper,
        JwtTokenProvider jwtTokenProvider,
        ArticleService articleService,
        UserFavoriteMapper userFavoriteMapper,
        UserLikeMapper userLikeMapper,
        CommentMapper commentMapper
    ) {
        this.blogUserMapper = blogUserMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.articleService = articleService;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userLikeMapper = userLikeMapper;
        this.commentMapper = commentMapper;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (blogUserMapper.selectByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        BlogUser user = new BlogUser();
        user.setUsername(username);
        user.setPasswordHash(bCryptPasswordEncoder.encode(request.getPassword()));
        user.setNickname(defaultText(request.getNickname(), username));
        user.setEmail(normalizeText(request.getEmail()));
        user.setAvatar("");
        user.setBio("");
        user.setRole(ROLE_USER);
        user.setStatus(1);
        blogUserMapper.insert(user);
        blogUserMapper.updateLastLoginAt(user.getId());

        String token = jwtTokenProvider.generateToken(username, ROLE_USER);
        return new LoginResponse(token, user.getNickname());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        BlogUser user = blogUserMapper.selectByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || !ROLE_USER.equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        blogUserMapper.updateLastLoginAt(user.getId());
        String token = jwtTokenProvider.generateToken(user.getUsername(), ROLE_USER);
        return new LoginResponse(token, user.getNickname());
    }

    public FrontUserProfileResponse me(String authorization) {
        return toProfile(requireUser(authorization));
    }

    @Transactional
    public FrontUserProfileResponse updateProfile(String authorization, FrontUserProfileUpdateRequest request) {
        BlogUser user = requireUser(authorization);
        user.setNickname(normalizeText(request.getNickname()));
        user.setEmail(normalizeText(request.getEmail()));
        user.setAvatar(normalizeText(request.getAvatar()));
        user.setBio(normalizeText(request.getBio()));
        blogUserMapper.updateProfile(user);
        return toProfile(blogUserMapper.selectById(user.getId()));
    }

    @Transactional
    public void updatePassword(String authorization, FrontUserPasswordUpdateRequest request) {
        BlogUser user = requireUser(authorization);
        if (!bCryptPasswordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }
        blogUserMapper.updatePassword(user.getId(), bCryptPasswordEncoder.encode(request.getNewPassword()));
    }

    public UserCenterResponse center(String authorization) {
        BlogUser user = requireUser(authorization);
        List<ArticleResponse> favorites = userFavoriteMapper.selectPublishedFavoritesByUserId(user.getId()).stream()
            .map(articleService::toResponse)
            .toList();
        List<ArticleResponse> likes = userLikeMapper.selectPublishedLikesByUserId(user.getId()).stream()
            .map(articleService::toResponse)
            .toList();
        List<CommentResponse> comments = commentMapper.selectByUserId(user.getId(), 20).stream()
            .map(this::toCommentResponse)
            .toList();
        UserActivityStatsResponse stats = new UserActivityStatsResponse(
            userFavoriteMapper.countByUserId(user.getId()),
            userLikeMapper.countByUserId(user.getId()),
            commentMapper.countByUserId(user.getId()),
            commentMapper.countByUserIdAndStatus(user.getId(), 0),
            commentMapper.countByUserIdAndStatus(user.getId(), 1)
        );
        return new UserCenterResponse(toProfile(user), stats, favorites, likes, comments);
    }

    public BlogUser requireUser(String authorization) {
        String token = resolveBearerToken(authorization);
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login expired");
        }

        String role = jwtTokenProvider.parseRole(token);
        if (!ROLE_USER.equals(role)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity");
        }

        String username = jwtTokenProvider.parseUsername(token);
        BlogUser user = blogUserMapper.selectByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || !ROLE_USER.equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or disabled");
        }
        return user;
    }

    public BlogUser optionalUser(String authorization) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            return requireUser(authorization);
        } catch (ResponseStatusException ignored) {
            return null;
        }
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return token;
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultText(String value, String fallback) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private FrontUserProfileResponse toProfile(BlogUser user) {
        return new FrontUserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getEmail() == null ? "" : user.getEmail(),
            user.getAvatar() == null ? "" : user.getAvatar(),
            user.getBio() == null ? "" : user.getBio()
        );
    }

    private CommentResponse toCommentResponse(com.blog.entity.Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getArticleId(),
            comment.getParentId(),
            comment.getArticleTitle(),
            comment.getNickname(),
            comment.getEmail(),
            comment.getContent(),
            comment.getStatus(),
            comment.getCreatedAt()
        );
    }
}
