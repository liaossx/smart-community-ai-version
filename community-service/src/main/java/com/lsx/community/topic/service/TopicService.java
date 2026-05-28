package com.lsx.community.topic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.community.topic.dto.TopicAuditDTO;
import com.lsx.community.topic.dto.TopicCommentDTO;
import com.lsx.community.topic.dto.TopicCreateDTO;
import com.lsx.community.topic.entity.Topic;
import com.lsx.community.topic.vo.TopicCommentVO;
import com.lsx.community.topic.vo.TopicVO;

import java.util.List;

public interface TopicService extends IService<Topic> {
    Long createTopic(TopicCreateDTO dto, Long userId);
    Page<TopicVO> listTopics(Long communityId, Long userId, String status, Integer pageNum, Integer pageSize);
    TopicVO getTopicDetail(Long topicId, Long userId);
    Boolean likeTopic(Long topicId, Long userId);
    Long commentTopic(TopicCommentDTO dto, Long userId);
    List<TopicCommentVO> getComments(Long topicId);
    Boolean auditTopic(TopicAuditDTO dto);
}
