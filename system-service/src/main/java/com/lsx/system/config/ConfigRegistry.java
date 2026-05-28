package com.lsx.system.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigRegistry {
    public static List<ConfigMeta> all() {
        List<ConfigMeta> list = new ArrayList<>();
        list.add(new ConfigMeta("notice.default.expire.days", "int", "0", "property-notice", "公告默认过期天数"));
        list.add(new ConfigMeta("notice.default.top", "bool", "false", "property-notice", "公告默认是否置顶"));
        list.add(new ConfigMeta("notice.expire.required.published", "bool", "false", "property-notice", "发布公告时是否必须设置过期时间"));
        list.add(new ConfigMeta("notice.top.allowed", "bool", "true", "property-notice", "是否允许公告置顶"));
        list.add(new ConfigMeta("notice.page.size.max", "int", "50", "property-notice", "公告列表最大分页大小"));
        list.add(new ConfigMeta("notice.title.length.max", "int", "100", "property-notice", "公告标题最大长度"));
        list.add(new ConfigMeta("notice.content.length.max", "int", "20000", "property-notice", "公告内容最大长度"));
        return list;
    }
}
