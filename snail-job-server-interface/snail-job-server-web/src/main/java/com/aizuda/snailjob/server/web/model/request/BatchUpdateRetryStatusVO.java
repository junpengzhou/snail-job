package com.aizuda.snailjob.server.web.model.request;

import com.aizuda.snailjob.model.request.base.StatusUpdateRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    /**
     * 待更新的重试任务列表
     */
    @NotEmpty(message = "At least one item must be selected")
    @Size(max = 100, message = "A maximum of 100 can be updated")
    private List<StatusUpdateRequest> updateRequestList;
}
