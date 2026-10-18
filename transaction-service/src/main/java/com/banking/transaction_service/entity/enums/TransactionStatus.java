package com.banking.transaction_service.entity.enums;

/*
* Transaction Lifecycle Flow:
*
* PENDING -> PROCESSING -> COMPLETED (clean transaction)
*                       -> PENDING_VERIFICATION (suspicious activity detected)
*                                    -> COMPLETED (After verification)
*                                    -> FLAGGED (SAGA REFUND)
*                       -> FAILED
*                       -> FLAGGED
* */


public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED

}
