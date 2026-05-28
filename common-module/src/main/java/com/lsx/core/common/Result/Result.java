package com.lsx.core.common.Result;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 状态码：200成功、400失败、401未登录
    private String msg;  // 提示信息
    private T data;      // 业务数据

    // 成功方法（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 成功方法（不带数据）
    public static <T> Result<T> success() {
        return success(null);
    }

    // 失败方法
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 未登录方法
    public static <T> Result<T> unauth() {
        Result<T> result = new Result<>();
        result.setCode(401);
        result.setMsg("请先登录");
        result.setData(null);
        return result;
    }
}