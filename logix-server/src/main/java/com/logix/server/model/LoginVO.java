package com.logix.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求响应
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginVO {
    private String username;
    private String password;
    private String message;

    public LoginVO(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public LoginVO(String message) {
        this.message = message;
    }
}
