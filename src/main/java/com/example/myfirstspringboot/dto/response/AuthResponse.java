package com.example.myfirstspringboot.dto.response;

import lombok.Data;

/**
 * 认证响应 DTO（登录/注册通用）
 */
@Data
public class AuthResponse {
    /**
     * 用户 ID（UUID）
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token 类型
     */
    private String tokenType;

    /**
     * 当前看板信息
     */
    private BoardInfo currentBoard;

    /**
     * 看板信息
     */
    @Data
    public static class BoardInfo {
        /**
         * 看板 ID（UUID）
         */
        private String boardId;

        /**
         * 看板名称
         */
        private String boardName;

        /**
         * 是否有看板
         */
        private boolean hasBoard;
    }
}
