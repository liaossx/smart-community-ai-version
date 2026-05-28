package com.lsx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.user.entity.UserRegisterRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRegisterRequestMapper extends BaseMapper<UserRegisterRequest> {
}
