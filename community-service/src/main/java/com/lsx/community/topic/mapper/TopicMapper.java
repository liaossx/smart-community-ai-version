package com.lsx.community.topic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.community.topic.entity.Topic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
}
