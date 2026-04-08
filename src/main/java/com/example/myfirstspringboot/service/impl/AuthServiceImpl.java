package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.User;
import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoginRequest;
import com.example.myfirstspringboot.dto.request.RegisterRequest;
import com.example.myfirstspringboot.dto.response.AuthResponse;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.exception.BusinessException;
import com.example.myfirstspringboot.mapper.BoardMapper;
import com.example.myfirstspringboot.mapper.UserMapper;
import com.example.myfirstspringboot.service.AuthService;
import com.example.myfirstspringboot.service.BoardService;
import com.example.myfirstspringboot.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 认证服务实现类
 * <p>
 * 处理用户登录、注册等认证相关业务逻辑
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BoardService boardService;
    private final BoardMapper boardMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     * <p>
     * 业务逻辑：
     * 1. 验证用户名和密码
     * 2. 生成 JWT Token
     * 3. 确保用户有看板（没有则自动创建）
     * </p>
     *
     * @param request 登录请求参数
     * @return 认证响应（包含用户信息、Token 和看板信息）
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 参数验证
        validateLoginRequest(request);

        String username = request.getUsername().trim();
        String password = request.getPassword();

        log.info("用户登录：username={}", username);

        // 查找用户
        User user = findUserByUsername(username);

        // 验证密码
        validatePassword(password, user, username);

        // 确保用户有看板
        AuthResponse.BoardInfo boardInfo = ensureUserHasBoard(user);

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId());

        // 构建响应
        return buildAuthResponse(user, token, boardInfo);
    }

    /**
     * 用户注册
     * <p>
     * 业务逻辑：
     * 1. 验证请求参数
     * 2. 检查用户名是否已存在
     * 3. 创建新用户（密码加密）
     * 4. 生成 JWT Token
     * 5. 创建默认看板
     * </p>
     *
     * @param request 注册请求参数
     * @return 认证响应（包含用户信息、Token 和看板信息）
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 参数验证
        validateRegisterRequest(request);

        String username = request.getUsername().trim();
        String password = request.getPassword();

        // 检查用户名是否已存在
        if (userMapper.existsByUsername(username)) {
            throw new BusinessException(409, "用户名已存在");
        }

        log.info("用户注册：username={}", username);

        // 创建新用户
        String userId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(password);

        User newUser = User.builder()
                .id(userId)
                .username(username)
                .passwordHash(passwordHash)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : username)
                .email(request.getEmail())
                .build();

        userMapper.insert(newUser);
        log.info("用户注册成功：userId={}, username={}", userId, username);

        // 创建默认看板
        CreateBoardRequest boardRequest = new CreateBoardRequest();
        boardRequest.setName(username + " 的求职看板");
        BoardDto defaultBoard = boardService.createBoard(userId, boardRequest);
        log.info("为用户 {} 创建默认看板：{}", userId, defaultBoard.getId());

        // 生成 JWT Token
        String token = jwtUtil.generateToken(userId);

        // 构建看板信息
        AuthResponse.BoardInfo boardInfo = new AuthResponse.BoardInfo();
        boardInfo.setBoardId(defaultBoard.getId().toString());
        boardInfo.setBoardName(defaultBoard.getName());
        boardInfo.setHasBoard(true);

        // 构建响应
        return buildAuthResponse(newUser, token, boardInfo);
    }

    // ========== 辅助方法 ==========

    /**
     * 验证登录请求参数
     */
    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(400, "登录请求不能为空");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BusinessException(400, "密码不能为空");
        }
    }

    /**
     * 验证注册请求参数
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(400, "注册请求不能为空");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException(400, "密码不能为空且长度至少 6 位");
        }
    }

    /**
     * 根据用户名查找用户
     */
    private User findUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("登录失败：用户不存在：username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        return user;
    }

    /**
     * 验证密码
     */
    private void validatePassword(String password, User user, String username) {
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("登录失败：密码错误：username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    /**
     * 确保用户有看板，没有则自动创建
     */
    private AuthResponse.BoardInfo ensureUserHasBoard(User user) {
        // 查询用户是否有看板
        Board board = boardMapper.selectFirstByUserIdOrderByCreatedAtAsc(user.getId());

        if (board != null) {
            // 用户已有看板
            return buildBoardInfo(board.getId().toString(), board.getName());
        } else {
            // 用户没有看板，自动创建
            log.info("用户 {} 没有看板，自动创建默认看板", user.getId());
            CreateBoardRequest boardRequest = new CreateBoardRequest();
            boardRequest.setName(user.getUsername() + " 的求职看板");
            BoardDto newBoard = boardService.createBoard(user.getId(), boardRequest);
            return buildBoardInfo(newBoard.getId().toString(), newBoard.getName());
        }
    }

    /**
     * 构建看板信息
     */
    private AuthResponse.BoardInfo buildBoardInfo(String boardId, String boardName) {
        AuthResponse.BoardInfo info = new AuthResponse.BoardInfo();
        info.setBoardId(boardId);
        info.setBoardName(boardName);
        info.setHasBoard(true);
        return info;
    }

    /**
     * 构建认证响应
     */
    private AuthResponse buildAuthResponse(User user, String token, AuthResponse.BoardInfo boardInfo) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setCurrentBoard(boardInfo);
        return response;
    }
}
