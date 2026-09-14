package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean isUsernameExists(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return userMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public User register(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        // 使用 MyBatis-Plus BaseMapper 完成插入，无需手写 SQL
        userMapper.insert(user);
        return user;
    }

    @Override
    public User loginByUsernameAndPassword(String username, String password) {
        User user = getByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone.trim());
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public User registerThirdPartyUser(String username, String password, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        userMapper.insert(user);
        return user;
    }

    @Override
    public void updatePhone(Long userId, String phone) {
        if (userId == null || phone == null || phone.trim().isEmpty()) {
            return;
        }
        User user = new User();
        user.setId(userId);
        user.setPhone(phone.trim());
        userMapper.updateById(user);
    }

    @Override
    public User updateUsername(String currentUsername, String newUsername) {
        User user = requireByUsername(currentUsername);

        String target = (newUsername == null ? "" : newUsername).trim();
        if (target.isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        // 新用户名与当前用户名相同，无需修改
        if (target.equals(user.getUsername())) {
            return user;
        }
        // 重名检查：被其他用户占用则拒绝
        User other = getByUsername(target);
        if (other != null && !other.getId().equals(user.getId())) {
            throw new BusinessException("用户名已被使用，请换一个");
        }

        // 只更新 username 字段，MyBatis-Plus 默认忽略 null 字段，不会误改密码/手机号
        User update = new User();
        update.setId(user.getId());
        update.setUsername(target);
        userMapper.updateById(update);

        user.setUsername(target);
        return user;
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = requireByUsername(username);
        if (oldPassword == null || !user.getPassword().equals(oldPassword)) {
            throw new BusinessException("旧密码不正确");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        User update = new User();
        update.setId(user.getId());
        update.setPassword(newPassword);
        userMapper.updateById(update);
    }

    @Override
    public boolean isUsernameAvailable(String currentUsername, String candidateUsername) {
        String target = (candidateUsername == null ? "" : candidateUsername).trim();
        if (target.isEmpty()) {
            return false;
        }
        // 和自己当前用户名一样，当然可用
        if (target.equals(currentUsername)) {
            return true;
        }
        return !isUsernameExists(target);
    }

    private User requireByUsername(String username) {
        User user = getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        return user;
    }
}
