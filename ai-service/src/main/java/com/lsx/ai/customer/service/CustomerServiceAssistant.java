package com.lsx.ai.customer.service;

import com.lsx.ai.customer.dto.CustomerServiceAnswerResponse;
import com.lsx.ai.customer.dto.CustomerServiceAskRequest;

public interface CustomerServiceAssistant {
    CustomerServiceAnswerResponse answer(CustomerServiceAskRequest request);
}
