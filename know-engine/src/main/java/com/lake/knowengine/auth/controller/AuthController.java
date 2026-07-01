package com.lake.knowengine.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // todo 测试账号
    private static final String STAFF_EMP_ID = "10001";
    // todo 测试密码
    private static final String STAFF_PASSWORD = "123456";

    private static final String USER_PHONE = "13800138000";
    private static final String USER_PASSWORD = "123456";

    @PostMapping("/staffLogin")
    public SaResult staffLogin(@RequestBody StaffLoginRequest request) {
        if (!STAFF_EMP_ID.equals(request.empId()) || !STAFF_PASSWORD.equals(request.password())) {
            return SaResult.error("工号或密码错误");
        }
        StpUtil.login("staff:" + request.empId());
        StpUtil.getSession().set("userInfo", userInfo("staff", request.empId(), "测试员工"));
        return SaResult.ok("登录成功");
    }

    @PostMapping("/login")
    public SaResult login(@RequestBody UserLoginRequest request) {
        if (!USER_PHONE.equals(request.phone()) || !USER_PASSWORD.equals(request.password())) {
            return SaResult.error("手机号或密码错误");
        }
        StpUtil.login("user:" + request.phone());
        StpUtil.getSession().set("userInfo", userInfo("user", request.phone(), "测试用户"));
        return SaResult.ok("登录成功");
    }

    @GetMapping("/isLogin")
    public SaResult isLogin() {
        return SaResult.data(StpUtil.isLogin());
    }

    @GetMapping("/userInfo")
    public SaResult userInfo() {
        if (!StpUtil.isLogin()) {
            return SaResult.error("未登录");
        }
        return SaResult.data(StpUtil.getSession().get("userInfo"));
    }

    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("退出成功");
    }

    private Map<String, Object> userInfo(String userType, String account, String name) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userType", userType);
        userInfo.put("account", account);
        userInfo.put("name", name);
        return userInfo;
    }

    public record StaffLoginRequest(String empId, String password) {
    }

    public record UserLoginRequest(String phone, String password) {
    }
}
