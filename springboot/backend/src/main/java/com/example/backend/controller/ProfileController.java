package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.ChangePasswordDTO;
import com.example.backend.dto.ProfileVO;
import com.example.backend.dto.UpdateUsernameDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import com.example.backend.util.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 个人中心：查看资料、修改用户名、修改密码。
 * 所有接口由 JwtInterceptor 保护，当前用户名从请求属性中取得。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /** 获取当前登录用户资料（不返回密码） */
    @GetMapping
    public Result<ProfileVO> getProfile(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error(401, "用户不存在");
        }
        ProfileVO vo = new ProfileVO();
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        return Result.success(vo);
    }

    /**
     * 修改邮箱（用于定时提醒推送）。
     * 传空串表示清除邮箱。更新成功后不再接收邮件提醒。
     */
    @PutMapping("/email")
    public Result<Void> updateEmail(@RequestBody java.util.Map<String, String> body,
                                     HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        String email = body == null ? null : body.get("email");
        userService.updateEmail(username, email);
        return Result.success("邮箱设置成功", null);
    }

    /**
     * 实时检查用户名是否可用（前端输入框查重用）。
     * 返回 data=true 表示可用。
     */
    @GetMapping("/username-available")
    public Result<Boolean> usernameAvailable(@RequestParam("username") String username,
                                             HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("username");
        return Result.success(userService.isUsernameAvailable(currentUsername, username));
    }

    /**
     * 修改用户名。
     * 用户名变更后旧 Token 中的用户名将失效，因此重新签发 Token 返回。
     */
    @PutMapping("/username")
    public Result<UserDTO> updateUsername(@RequestBody UpdateUsernameDTO dto,
                                          HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("username");
        User updated = userService.updateUsername(currentUsername, dto == null ? null : dto.getNewUsername());

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(updated.getUsername());
        userDTO.setToken(JwtUtil.createToken(updated));
        return Result.success("用户名修改成功", userDTO);
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto,
                                       HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (dto == null) {
            return Result.error(400, "参数不能为空");
        }
        userService.changePassword(username, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }
}
