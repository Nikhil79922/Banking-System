package com.banking.common.dto.commonRes;

import java.time.LocalDateTime;

public record SuccessResponse<T>(
         boolean success,
         String message,
         T data,
         LocalDateTime timestamp,
         int status) {
}
