package com.example.backend.service.impl;

import com.example.backend.dto.UserDTO;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.AuthService;
import com.example.backend.service.UserService;
import com.example.backend.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    public AuthServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDTO register(UserDTO userDTO) {
        String username = normalizeUsername(userDTO);
        String password = normalizePassword(userDTO);

        if (userService.isUsernameExists(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = userService.register(username, password);
        return buildLoginResult(user);
    }

    @Override
    public UserDTO login(UserDTO userDTO) {
        String username = normalizeUsername(userDTO);
        String password = normalizePassword(userDTO);

        User user = userService.loginByUsernameAndPassword(username, password);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return buildLoginResult(user);
    }

    @Override
    public UserDTO getUserInfoByToken(String token) {
        String username = JwtUtil.getUsername(token);
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，Token 无效");
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(user.getUsername());
        userDTO.setToken(token);
        return userDTO;
    }

    /** 校验用户名并去除首尾空格 */
    private String normalizeUsername(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUsername() == null
                || userDTO.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        return userDTO.getUsername().trim();
    }

    /** 校验密码 */
    private String normalizePassword(UserDTO userDTO) {
        if (userDTO == null || userDTO.getPassword() == null
                || userDTO.getPassword().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        return userDTO.getPassword();
    }

    /** 构建返回给前端的 DTO：只含用户名和 Token，不含密码 */
    private UserDTO buildLoginResult(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(user.getUsername());
        userDTO.setToken(JwtUtil.createToken(user));
        return userDTO;
    }
}
