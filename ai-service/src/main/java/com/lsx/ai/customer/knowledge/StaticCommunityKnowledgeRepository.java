package com.lsx.ai.customer.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "smart-community.ai.customer-service", name = "knowledge-store",
        havingValue = "static", matchIfMissing = true)
public class StaticCommunityKnowledgeRepository implements CommunityKnowledgeRepository {
    /*
     * RAG v1 的知识库来源。
     *
     * 当前为了学习 RAG 链路，先把公告、制度、流程写成内存数据。
     * 后续可以把这个类替换成 MySQL 查询版本，或者替换成向量数据库检索版本。
     */
    private final List<KnowledgeDocument> documents = List.of(
            new KnowledgeDocument(
                    "PROCESS_REPAIR_001",
                    "REPAIR_PROCESS",
                    "居民报修处理流程",
                    "居民可在小程序或业主端提交报修，填写故障类型、故障描述、房屋信息和图片。物业受理后生成工单，维修人员接单并上门处理。紧急漏水、漏电、电梯困人、燃气异味等情况应优先电话联系物业值班人员。",
                    List.of("报修", "维修", "工单", "漏水", "漏电", "上门", "水管", "电路", "电梯困人"),
                    null
            ),
            new KnowledgeDocument(
                    "POLICY_REPAIR_001",
                    "PROPERTY_POLICY",
                    "维修响应时效制度",
                    "普通报修建议在24小时内响应；较急问题建议在2小时内响应；漏水、漏电、电梯故障、消防隐患等紧急问题建议30分钟内响应；涉及人身安全的高危事件应立即人工确认并联系应急人员。",
                    List.of("响应", "时效", "24小时", "2小时", "30分钟", "紧急", "漏水", "漏电", "消防", "高危"),
                    null
            ),
            new KnowledgeDocument(
                    "NOTICE_WATER_001",
                    "COMMUNITY_NOTICE",
                    "二次供水水箱清洗停水通知",
                    "因二次供水水箱清洗，3栋和4栋计划在本周六09:00至12:00暂停供水。请居民提前储水，停水期间关闭家中水龙头，恢复供水后可先短暂放水。",
                    List.of("停水", "供水", "水箱", "3栋", "4栋", "周六", "储水", "水龙头"),
                    1L
            ),
            new KnowledgeDocument(
                    "POLICY_FEE_001",
                    "PROPERTY_POLICY",
                    "物业费缴费与催缴说明",
                    "物业费可通过业主端线上缴纳，也可到物业服务中心线下缴纳。若产生欠费，系统会发送缴费提醒。对费用明细有疑问时，居民可联系物业前台核对房屋、周期、金额和缴费记录。",
                    List.of("物业费", "缴费", "欠费", "催缴", "费用", "发票", "明细", "线上缴纳"),
                    null
            ),
            new KnowledgeDocument(
                    "PROCESS_COMPLAINT_001",
                    "PROPERTY_PROCESS",
                    "居民投诉处理流程",
                    "居民可提交投诉建议，物业客服登记后分派给相关岗位处理。一般投诉应在1个工作日内受理，处理完成后记录处理结果。涉及安全、秩序、噪音等问题应保留现场信息和时间描述。",
                    List.of("投诉", "建议", "噪音", "秩序", "安全", "处理结果", "客服", "受理"),
                    null
            ),
            new KnowledgeDocument(
                    "POLICY_VISITOR_001",
                    "PROPERTY_POLICY",
                    "访客登记制度",
                    "访客进入小区前应进行访客登记，填写来访人、被访人、手机号和来访时间。门岗核验后放行。长期施工、装修人员应按物业要求办理临时出入手续。",
                    List.of("访客", "登记", "门岗", "来访", "装修", "施工", "出入"),
                    null
            )
    );

    @Override
    public List<KnowledgeDocument> findAll() {
        // Retriever 会从这里拿到全部候选资料，再根据用户问题做相关性排序。
        return documents;
    }
}
