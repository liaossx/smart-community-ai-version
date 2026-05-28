package com.lsx.community.topic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.community.client.UserServiceClient;
import com.lsx.community.dto.external.UserInfoDTO;
import com.lsx.community.topic.dto.TopicAuditDTO;
import com.lsx.community.topic.dto.TopicCommentDTO;
import com.lsx.community.topic.dto.TopicCreateDTO;
import com.lsx.community.topic.entity.Topic;
import com.lsx.community.topic.entity.TopicComment;
import com.lsx.community.topic.entity.TopicLike;
import com.lsx.community.topic.enums.TopicStatusEnum;
import com.lsx.community.topic.mapper.TopicCommentMapper;
import com.lsx.community.topic.mapper.TopicLikeMapper;
import com.lsx.community.topic.mapper.TopicMapper;
import com.lsx.community.topic.service.TopicService;
import com.lsx.community.topic.vo.TopicCommentVO;
import com.lsx.community.topic.vo.TopicVO;
import com.lsx.core.common.Util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Autowired
    private TopicCommentMapper commentMapper;

    @Autowired
    private TopicLikeMapper likeMapper;

    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    @Transactional
    public Long createTopic(TopicCreateDTO dto, Long userId) {
        Topic topic = new Topic();
        BeanUtils.copyProperties(dto, topic);
        topic.setUserId(userId);
        topic.setCommunityId(UserContext.getCommunityId());
        topic.setStatus(TopicStatusEnum.PENDING.getCode());
        topic.setViewCount(0);
        topic.setLikeCount(0);
        topic.setCommentCount(0);
        topic.setCreateTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(topic);
        return topic.getId();
    }

    @Override
    public Page<TopicVO> listTopics(Long communityId, Long userId, String status, Integer pageNum, Integer pageSize) {
        Page<Topic> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Topic> query = new LambdaQueryWrapper<>();
        String statusCode = TopicStatusEnum.APPROVED.getCode();
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusCode = TopicStatusEnum.valueOf(status.trim().toUpperCase()).getCode();
            } catch (Exception ignored) {
                statusCode = status.trim();
            }
        }
        query.eq(Topic::getCommunityId, communityId)
                .eq(Topic::getStatus, statusCode)
                .orderByDesc(Topic::getCreateTime);

        Page<Topic> topicPage = baseMapper.selectPage(page, query);

        Page<TopicVO> result = new Page<>(pageNum, pageSize);
        result.setTotal(topicPage.getTotal());

        List<TopicVO> vOs = topicPage.getRecords().stream().map(topic -> {
            TopicVO vo = convertToVO(topic);
            if (userId != null) {
                vo.setIsLiked(isUserLiked(topic.getId(), userId));
            }
            return vo;
        }).collect(Collectors.toList());

        result.setRecords(vOs);
        return result;
    }

    @Override
    public TopicVO getTopicDetail(Long topicId, Long userId) {
        Topic topic = baseMapper.selectById(topicId);
        if (topic == null) return null;

        // 增加浏览量
        topic.setViewCount(topic.getViewCount() + 1);
        baseMapper.updateById(topic);

        TopicVO vo = convertToVO(topic);
        if (userId != null) {
            vo.setIsLiked(isUserLiked(topicId, userId));
        }
        return vo;
    }

    @Override
    @Transactional
    public Boolean likeTopic(Long topicId, Long userId) {
        LambdaQueryWrapper<TopicLike> query = new LambdaQueryWrapper<>();
        query.eq(TopicLike::getTopicId, topicId).eq(TopicLike::getUserId, userId);
        TopicLike exist = likeMapper.selectOne(query);

        Topic topic = baseMapper.selectById(topicId);
        if (topic == null) return false;

        if (exist == null) {
            TopicLike like = new TopicLike();
            like.setTopicId(topicId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            likeMapper.insert(like);
            topic.setLikeCount(topic.getLikeCount() + 1);
        } else {
            likeMapper.deleteById(exist.getId());
            topic.setLikeCount(Math.max(0, topic.getLikeCount() - 1));
        }
        baseMapper.updateById(topic);
        return true;
    }

    @Override
    @Transactional
    public Long commentTopic(TopicCommentDTO dto, Long userId) {
        Topic topic = baseMapper.selectById(dto.getTopicId());
        if (topic == null) throw new RuntimeException("话题不存在");

        TopicComment comment = new TopicComment();
        BeanUtils.copyProperties(dto, comment);
        comment.setUserId(userId);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        topic.setCommentCount(topic.getCommentCount() + 1);
        baseMapper.updateById(topic);

        return comment.getId();
    }

    @Override
    public List<TopicCommentVO> getComments(Long topicId) {
        LambdaQueryWrapper<TopicComment> query = new LambdaQueryWrapper<>();
        query.eq(TopicComment::getTopicId, topicId).orderByDesc(TopicComment::getCreateTime);
        List<TopicComment> comments = commentMapper.selectList(query);

        if (comments.isEmpty()) return Collections.emptyList();

        return comments.stream().map(comment -> {
            TopicCommentVO vo = new TopicCommentVO();
            vo.setId(comment.getId());
            vo.setContent(comment.getContent());
            vo.setUserId(comment.getUserId());
            vo.setCreateTime(comment.getCreateTime());

            UserInfoDTO userInfo = userServiceClient.getUserById(comment.getUserId());
            if (userInfo != null) {
                vo.setUserName(userInfo.getName());
                // vo.setUserAvatar(user.getAvatar());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean auditTopic(TopicAuditDTO dto) {
        Topic topic = baseMapper.selectById(dto.getTopicId());
        if (topic == null) return false;

        UserInfoDTO admin = userServiceClient.getUserById(dto.getAdminId());
        if (admin == null) return false;

        topic.setStatus(dto.getStatus());
        topic.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(topic);

        log.info("话题[{}]已被管理员[{}]审核为[{}]", topic.getTitle(), admin.getName(), dto.getStatus());
        return true;
    }

    private TopicVO convertToVO(Topic topic) {
        TopicVO vo = new TopicVO();
        BeanUtils.copyProperties(topic, vo);

        if (topic.getImages() != null && !topic.getImages().isEmpty()) {
            vo.setImages(Arrays.asList(topic.getImages().split(",")));
        }

        UserInfoDTO userInfo = userServiceClient.getUserById(topic.getUserId());
        if (userInfo != null) {
            vo.setUserName(userInfo.getName());
            // vo.setUserAvatar(user.getAvatar());
        }

        return vo;
    }

    private Boolean isUserLiked(Long topicId, Long userId) {
        LambdaQueryWrapper<TopicLike> query = new LambdaQueryWrapper<>();
        query.eq(TopicLike::getTopicId, topicId).eq(TopicLike::getUserId, userId);
        return likeMapper.selectCount(query) > 0;
    }
}
