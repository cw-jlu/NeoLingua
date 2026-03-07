package com.speakmaster.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.JwtUtil;
import com.speakmaster.user.dto.LoginRequest;
import com.speakmaster.user.dto.RegisterRequest;
import com.speakmaster.user.dto.UserDTO;
import com.speakmaster.user.entity.PointsRecord;
import com.speakmaster.user.entity.User;
import com.speakmaster.user.mapper.PointsRecordMapper;
import com.speakmaster.user.mapper.UserMapper;
import com.speakmaster.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(RegisterRequest request) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null) {
            Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail())
                    .eq(User::getDeleted, 0));
            if (emailCount > 0) {
                throw new BusinessException(3010, "邮箱已被注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPoints(0L);
        user.setStatus(0);

        userMapper.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        return convertToDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String login(LoginRequest request, String ip) {
        // 查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0));
        
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getStatus() == 2) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now().format(FORMATTER));
        user.setLastLoginIp(ip);
        userMapper.updateById(user);

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("用户登录成功: userId={}, username={}, ip={}", user.getId(), user.getUsername(), ip);

        return token;
    }

    @Override
    @Cacheable(value = "user", key = "#userId")
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public UserDTO updateUser(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 更新字段
        if (userDTO.getNickname() != null) {
            user.setNickname(userDTO.getNickname());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        if (userDTO.getAvatar() != null) {
            user.setAvatar(userDTO.getAvatar());
        }
        if (userDTO.getGender() != null) {
            user.setGender(userDTO.getGender());
        }
        if (userDTO.getBirthday() != null) {
            user.setBirthday(userDTO.getBirthday());
        }
        if (userDTO.getBio() != null) {
            user.setBio(userDTO.getBio());
        }

        userMapper.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);

        return convertToDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.markDeleted();
        userMapper.updateById(user);
        log.info("用户删除成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void addPoints(Long userId, Long points, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPoints(user.getPoints() + points);
        userMapper.updateById(user);
        log.info("用户积分增加: userId={}, points={}, reason={}", userId, points, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void deductPoints(Long userId, Long points, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getPoints() < points) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }

        user.setPoints(user.getPoints() - points);
        userMapper.updateById(user);
        log.info("用户积分扣除: userId={}, points={}, reason={}", userId, points, reason);
    }

    @Override
    public Page<UserDTO> adminGetUserList(String keyword, Integer status, int page, int size) {
        Page<User> userPage = new Page<>(page + 1, size);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        
        userMapper.selectPage(userPage, wrapper);
        
        Page<UserDTO> dtoPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        dtoPage.setRecords(userPage.getRecords().stream().map(this::convertToDTO).toList());
        
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO adminCreateUser(UserDTO userDTO, String password) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userDTO.getUsername())
                .eq(User::getDeleted, 0));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(userDTO.getNickname() != null ? userDTO.getNickname() : userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setGender(userDTO.getGender() != null ? userDTO.getGender() : 0);
        user.setPoints(0L);
        user.setStatus(userDTO.getStatus() != null ? userDTO.getStatus() : 0);
        
        userMapper.insert(user);
        log.info("[管理端] 创建用户: userId={}, username={}", user.getId(), user.getUsername());
        
        return convertToDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public UserDTO adminUpdateUser(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (userDTO.getNickname() != null) user.setNickname(userDTO.getNickname());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        if (userDTO.getPhone() != null) user.setPhone(userDTO.getPhone());
        if (userDTO.getAvatar() != null) user.setAvatar(userDTO.getAvatar());
        if (userDTO.getGender() != null) user.setGender(userDTO.getGender());
        if (userDTO.getBirthday() != null) user.setBirthday(userDTO.getBirthday());
        if (userDTO.getBio() != null) user.setBio(userDTO.getBio());
        if (userDTO.getStatus() != null) user.setStatus(userDTO.getStatus());
        
        userMapper.updateById(user);
        log.info("[管理端] 更新用户: userId={}", userId);
        
        return convertToDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void adminBanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setStatus(1);
        userMapper.updateById(user);
        log.info("[管理端] 封禁用户: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void adminUnbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setStatus(0);
        userMapper.updateById(user);
        log.info("[管理端] 解封用户: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminResetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("[管理端] 重置密码: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void adminGrantPoints(Long userId, Long points, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPoints(user.getPoints() + points);
        userMapper.updateById(user);

        // 记录积分变动
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPointsChange(points);
        record.setPointsAfter(user.getPoints());
        record.setReason(reason != null ? reason : "管理员发放");
        record.setBusinessType(5); // 其他
        pointsRecordMapper.insert(record);

        log.info("[管理端] 发放积分: userId={}, points={}", userId, points);
    }

    @Override
    public Page<PointsRecord> adminGetPointsRecords(Long userId, int page, int size) {
        Page<PointsRecord> recordPage = new Page<>(page + 1, size);
        
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getDeleted, 0)
                .orderByDesc(PointsRecord::getCreateTime);
        
        return pointsRecordMapper.selectPage(recordPage, wrapper);
    }

    @Override
    public Map<String, Object> adminGetUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)));
        stats.put("normalUsers", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 0)
                .eq(User::getDeleted, 0)));
        stats.put("bannedUsers", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0)));
        stats.put("lockedUsers", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 2)
                .eq(User::getDeleted, 0)));
        
        return stats;
    }

    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setGender(user.getGender());
        dto.setBirthday(user.getBirthday());
        dto.setBio(user.getBio());
        dto.setPoints(user.getPoints());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        return dto;
    }
}
