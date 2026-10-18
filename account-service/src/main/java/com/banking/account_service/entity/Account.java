package com.banking.account_service.entity;

import com.banking.account_service.entity.enums.AccountStatus;
import com.banking.account_service.entity.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_email", columnList = "email"),
                @Index(name = "idx_accounts_phone", columnList = "phone"),
                @Index(name = "idx_accounts_status", columnList = "status"),
                @Index(name = "idx_accounts_account_type", columnList = "account_type")
        }
)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false , unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(nullable = false , precision = 15 , scale = 2)
    private BigDecimal balance;

    @Column(nullable = false , precision = 15 , scale = 2)
    private BigDecimal dailyTransactionLimit;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



}
