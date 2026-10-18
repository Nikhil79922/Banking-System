package com.banking.transaction_service.entity;


import com.banking.transaction_service.entity.enums.TransactionStatus;
import com.banking.transaction_service.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_referenceNumber",
                        columnNames = "referenceNumber"
                )
        },
        indexes = {
                @Index(
                        name = "idx_transaction_senderAccountNumber",
                        columnList = "senderAccountNumber"
                ),
                @Index(
                        name = "idx_transaction_receiverAccountNumber",
                        columnList = "receiverAccountNumber"
                ),
                @Index(
                        name = "idx_transaction_createdAt",
                        columnList = "createdAt"
                ),
                @Index(
                        name = "idx_transaction_status",
                        columnList = "status"
                )
        }
)
@Check(constraints = "amount > 0")
@Getter
@Setter
@ToString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String senderAccountNumber;

    @Column(nullable = false)
    private String receiverAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private TransactionStatus status;

    private String description;

    private String failureReason;

    @Column(nullable = false)
    private String referenceNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
