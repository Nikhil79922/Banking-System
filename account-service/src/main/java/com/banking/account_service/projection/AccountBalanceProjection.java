package com.banking.account_service.projection;

import java.math.BigDecimal;

public interface AccountBalanceProjection {

    String getAccountNumber();

    BigDecimal getBalance();
}
