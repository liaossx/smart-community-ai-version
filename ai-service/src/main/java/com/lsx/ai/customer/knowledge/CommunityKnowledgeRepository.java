package com.lsx.ai.customer.knowledge;

import java.util.List;

public interface CommunityKnowledgeRepository {
    List<KnowledgeDocument> findAll();
}
