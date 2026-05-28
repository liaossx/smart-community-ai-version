package com.lsx.property.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.property.notice.dto.*;
import com.lsx.property.notice.entity.SysNotice;
import java.util.List;

public interface NoticeService extends IService<SysNotice> {
    Long createNotice(NoticeCreateDTO dto, Long userId);

    Page<SysNotice> listNotices(String title, String status, Integer pageNum, Integer pageSize);
    
    Page<NoticeDTO> getUserNotices(Long userId, Integer pageNum, Integer pageSize);

    Boolean readNotice(Long noticeId, Long userId);

    Boolean deleteNotice(Long noticeId);

    SysNotice getNoticeById(Long id);

    Boolean updateNotice(Long id, NoticeCreateDTO dto);
    
    // 过期处理相关
    List<ExpiringNoticeDTO> getExpiringNotices(Integer days);
    
    Boolean batchExpireNotices(BatchNoticeExpireDTO dto);
    
    Integer getUnreadCount(Long userId);

    NoticeReadStatDTO getReadStat(Long noticeId);
}
