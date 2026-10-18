package com.banking.fraud_detection_service.mapper;
import com.banking.common.dto.commonRes.SuccessResponse;
import com.banking.common.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class FeignClientResponseMapper {

    public <T> T mapToDto(
            ResponseEntity<SuccessResponse<T>> response) {

        if (response.getBody() == null || response.getBody().data() == null) {
            throw new NotFoundException("Feign client request data not found");
        }

        return response.getBody().data();
    }
}
