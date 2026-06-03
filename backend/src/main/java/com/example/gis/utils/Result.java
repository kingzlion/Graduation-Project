package com.example.gis.utils;

public class Result<T> {
    private int code = 200;
    private String message = "success";
    private T data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.data = data;
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(message, 500);
    }

    public static <T> Result<T> error(String message, int code) {
        Result<T> r = new Result<>();
        r.message = message;
        r.code = code;
        return r;
    }
}
