package com.sky.controller.user;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.sky.context.BaseContext;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("user/user")
@Api(tags="C端用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 微信登陆 
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("微信登陆")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信用户登陆:{}",userLoginDTO.getCode());

        //微信登陆
        User user = userService.wxlogin(userLoginDTO);

        //为微信用户生成jwt令牌
        Map<String,Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);

    }

    /** 用户退出；JWT 无状态，前端还需删除本地 token。 */
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<String> logout(){
        BaseContext.removeCurrentId();
        return Result.success();
    }
    
}
