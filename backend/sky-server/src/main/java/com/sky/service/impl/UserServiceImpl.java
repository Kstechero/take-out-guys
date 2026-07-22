package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    /** 微信小程序登录凭证校验接口 */
    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信用户登录。
     * 首次登录时自动创建用户，非首次登录则直接返回已有用户。
     *
     * @param userLoginDTO 包含微信临时登录凭证 code
     * @return 登录用户信息
     */
    @Override
    public User wxlogin(UserLoginDTO userLoginDTO) {
        
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openid是否为空，如果为空表示登陆失败，抛出业务异常
        if (openid == null || openid.trim().isEmpty()) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        

        //判断当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);

        //如果是新用户。自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回这个用户对象
        return user;

        
    }

    private String getOpenid(String code){
        //获取当前微信用户的OpenId
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");    
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, map);

        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject == null ? null : jsonObject.getString("openid");
        return openid;
    }

    

}
