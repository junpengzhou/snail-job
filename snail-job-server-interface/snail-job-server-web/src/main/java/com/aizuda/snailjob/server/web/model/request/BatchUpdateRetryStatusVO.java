package com.aizuda.snailjob.server.web.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量删除重试数据
 *
 * @author: opensnail
 * @date : 2023-04-30 22:30
 */
@Data
public class BatchUpdateRetryStatusVO {
    private String groupName;

    private String sceneName;

    private String bizNo;

    private String idempotentId;

    /**
     * 待更新的状态
     */
    @NotNull
    private Integer retryStatus;

    /**
     * 要更新成的状态
     */
    @NotNull
    private Integer status;
}
