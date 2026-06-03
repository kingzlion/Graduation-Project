package com.example.gis.utils;

import com.example.gis.controller.AuthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getSession().getAttribute(AuthController.SESSION_KEY) != null) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/manage/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(), Map.of("code", 401, "message", "请先登录"));
        } else {
            response.sendRedirect("/sjgl.html");
        }
        return false;
    }
}
