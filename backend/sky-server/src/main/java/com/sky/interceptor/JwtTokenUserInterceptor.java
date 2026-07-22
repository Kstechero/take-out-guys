package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 浏览器跨域访问会先发送不携带 JWT 的 OPTIONS 预检请求。
        // 预检必须放行，真正的业务请求仍会继续进行 JWT 校验。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、校验令牌
        try {
            log.info("用户端 JWT 校验，请求路径：{}，token：{}", request.getRequestURI(), maskToken(token));
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户 id：{}", userId);
            BaseContext.setCurrentId(userId);
            //3、通过，放行
            return true;

        } catch (Exception ex) {
            //4、不通过，响应401状态码
            log.warn("用户端 JWT 校验失败，请求路径：{}，原因：{}", request.getRequestURI(), ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 16) {
            return "未提供";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }
}
