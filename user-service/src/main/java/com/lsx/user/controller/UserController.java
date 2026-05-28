package com.lsx.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.core.common.Util.JwtUtil;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.user.client.ParkingServiceClient;
import com.lsx.user.client.HouseServiceClient;
import com.lsx.user.dto.external.HouseDTO;
import com.lsx.user.dto.LoginDto;
import com.lsx.user.dto.RegisterDto;
import com.lsx.user.dto.UserHouseBindDTO;
import com.lsx.user.dto.UserInfoDTO;
import com.lsx.user.vo.LoginResult;
import com.lsx.user.entity.User;
import com.lsx.user.service.UserService;

import com.lsx.user.vo.RegisterResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCrypt;


@RestController
@RequestMapping("/api/user")
@Tag(name = "用户基础接口")
public class UserController {

    @Resource
    private UserService userService;
    
    @Resource
    private ParkingServiceClient parkingServiceClient;
    
    @Resource
    private HouseServiceClient houseServiceClient;
    
    @Resource  // 添加这个注入
    private JwtUtil jwtUtil;  // 确保JwtUtil类有@Component注解

    @Value("${internal.token:}")
    private String internalToken;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResult> login(
            @RequestBody LoginDto loginDto

    ) {
        try {
            LoginResult loginResult = userService.login(loginDto);
            return Result.success(loginResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserInfoDTO> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail("Token无效或已过期");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 1) 优先找默认绑定的房屋
        // 调用 house-service
        List<HouseDTO> houses = houseServiceClient.getHousesByUserId(userId);
        HouseDTO userHouse = null;
        if (houses != null && !houses.isEmpty()) {
            userHouse = houses.get(0); // 暂时取第一个
        }

        String community = "未绑定社区";
        String room = "未绑定房屋";
        if (userHouse != null) {
             community = userHouse.getCommunityName();
             room = userHouse.getBuildingNo() + "栋" + userHouse.getHouseNo();
        }

        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getRealName());
        dto.setCommunity(community);
        dto.setRoom(room);
        dto.setRole(user.getRole());

        return Result.success(dto);
    }

    @Operation(summary = "修改资料")
    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        String name = body.get("realName");
        if (name != null && name.trim().length() > 0) {
            user.setRealName(name.trim());
        }
        user.setUpdateTime(java.time.LocalDateTime.now());
        userService.updateById(user);
        return Result.success("更新成功");
        }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || newPwd == null) {
            return Result.fail("参数不足");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        boolean ok = jwtUtil.validatePassword(oldPwd, user.getPassword());
        if (!ok) {
            return Result.fail("旧密码错误");
        }
        user.setPassword(jwtUtil.encryptPassword(newPwd));
        user.setUpdateTime(java.time.LocalDateTime.now());
        userService.updateById(user);
        return Result.success("修改成功");
    }
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<RegisterResult> register(@RequestBody RegisterDto registerDto) {
        try {
            RegisterResult registerResult = userService.register(registerDto);
            return Result.success(registerResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "绑定用户与房屋")
    @PostMapping("/bindUserToHouse")
    public Result<String> bindUserToHouse(@RequestBody UserHouseBindDTO bindDTO) {
        Long userId = bindDTO.getUserId();
        Long houseId = bindDTO.getHouseId();
        try {
            userService.bindUserToHouse(userId, houseId);
            return Result.success("绑定成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/inner/{id}")
    public UserInfoDTO getUserById(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        if (user == null) return null;
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setCommunityId(user.getCommunityId());
        return dto;
    }

    @PutMapping("/inner/{id}/community")
    public Result<Boolean> updateUserCommunityId(
            @PathVariable("id") Long id,
            @RequestParam("communityId") Long communityId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        if (internalToken != null && !internalToken.isEmpty() && !internalToken.equals(token)) {
            return Result.fail("无权访问");
        }
        try {
            return Result.success(userService.updateCommunityIdIfEmpty(id, communityId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/inner/list/role")
    public Result<List<UserInfoDTO>> getUsersByRole(@RequestParam("role") String role) {
        List<User> users = userService.list(new LambdaQueryWrapper<User>().eq(User::getRole, role));
        List<UserInfoDTO> dtos = new ArrayList<>();
        for (User user : users) {
            UserInfoDTO dto = new UserInfoDTO();
            dto.setId(user.getId());
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setName(user.getRealName());
            dto.setPhone(user.getPhone());
            dto.setRole(user.getRole());
            dto.setCommunityId(user.getCommunityId());
            dtos.add(dto);
        }
        return Result.success(dtos);
    }

    @GetMapping("/count")
    public Long countUsers() {
        return userService.count();
    }
    
    @GetMapping("/count/role")
    public Long countUsersByRole(@RequestParam("role") String role) {
        return userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, role));
    }

    @GetMapping("/{id}/realname")
    public String getRealNameById(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        return user != null ? user.getRealName() : null;
    }

    @Operation(summary = "测试专用：批量生成压测Token")
    @GetMapping("/test/generateTokens")
    public Result<String> generateTokens() {
        try {
            java.io.File file = new java.io.File("D:\\jmeter_tokens.csv");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            
            // 批量生成 2000 个测试用户 Token
            for (long i = 10000; i < 12000; i++) {
                // 生成 Token：userId=i, username="test"+i, role="OWNER", communityId=1L
                String token = jwtUtil.generateToken(i, "test" + i, "OWNER", 1L);
                
                // 写入 CSV 文件，格式：userId,token
                writer.write(i + "," + token + "\n");
            }
            writer.close();
            return Result.success("成功生成2000个测试用户及Token，保存在 D:\\jmeter_tokens.csv");
        } catch (java.io.IOException e) {
            return Result.fail("生成失败：" + e.getMessage());
        }
    }
}

