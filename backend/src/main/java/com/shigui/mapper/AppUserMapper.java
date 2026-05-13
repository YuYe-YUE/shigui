package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    // 继承 BaseMapper 后已经具备基础增删改查能力，暂时不需要手写 SQL。
}
