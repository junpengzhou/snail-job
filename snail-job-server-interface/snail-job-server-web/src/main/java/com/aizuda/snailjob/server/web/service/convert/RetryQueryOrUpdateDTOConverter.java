package com.aizuda.snailjob.server.web.service.convert;

import com.aizuda.snailjob.server.web.model.dto.RetryQueryOrUpdateDTO;
import com.aizuda.snailjob.server.web.model.request.BatchUpdateRetryStatusVO;
import com.aizuda.snailjob.server.web.model.request.RetryQueryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RetryQueryOrUpdateDTOConverter {
    RetryQueryOrUpdateDTOConverter INSTANCE = Mappers.getMapper(RetryQueryOrUpdateDTOConverter.class);

    RetryQueryOrUpdateDTO convert(RetryQueryVO retryQueryVO);

    RetryQueryOrUpdateDTO convert(BatchUpdateRetryStatusVO retryQueryVO);
}
