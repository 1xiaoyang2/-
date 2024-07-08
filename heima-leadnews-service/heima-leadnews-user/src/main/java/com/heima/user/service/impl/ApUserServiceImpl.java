package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.user.dtos.LoginDto;
import com.heima.model.common.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService  {
    @Override
    public ResponseResult login(LoginDto dto) {
        //正常登录
        if(!StringUtils.isEmpty(dto.getPassword()) && !StringUtils.isEmpty(dto.getPhone())){
            ApUser apUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));
            if(apUser==null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"用户信息不存在");
            }

            //对比密码
            String pw = DigestUtils.md5DigestAsHex((dto.getPassword() + apUser.getSalt()).getBytes());
            if(!pw.equals(apUser.getPassword())){
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }

            //返回数据
            String token = AppJwtUtil.getToken(Long.valueOf(apUser.getId()));
            Map<String,Object> map = new HashMap<>();
            map.put("token",token);
            apUser.setPassword("");
            apUser.setSalt("");
            map.put("user",apUser);
            return ResponseResult.okResult(map);
        }else {
            //游客登录
            String token = AppJwtUtil.getToken(0L);
            Map<String,Object> map = new HashMap<>();
            map.put("token",token);
            return ResponseResult.okResult(map);
        }
    }
}
