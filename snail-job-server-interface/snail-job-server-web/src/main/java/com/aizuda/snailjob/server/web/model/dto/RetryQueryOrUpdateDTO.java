package com.aizuda.snailjob.server.web.model.dto;

import lombok.Data;

@Data
public class RetryQueryOrUpdateDTO {
    private String groupName;

    private String sceneName;

    private String bizNo;

    private String idempotentId;

    private Integer retryStatus;
}
