package com.banking.notification_service.service.strategyResolvers;

import com.banking.notification_service.dto.NotificationRequest;
import com.banking.notification_service.enums.NotificationTypes;
import org.springframework.stereotype.Service;
import com.banking.notification_service.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationResolver {

    private final Map<NotificationTypes, NotificationService>
            notificationStrategies;

    public NotificationResolver(
            List<NotificationService> notificationServices) {

        this.notificationStrategies = notificationServices.stream()
                .collect(Collectors.toMap(
                        NotificationService::getNotificationType,
                        Function.identity()
                ));
    }

    // Finds and returns the strategy for the given notification type.
    public NotificationService getNotificationStrategy(
            NotificationTypes notificationType) {

        NotificationService notificationService =
                notificationStrategies.get(notificationType);

        if (notificationService == null) {
            throw new IllegalArgumentException(
                    "Unsupported notification type: " + notificationType
            );
        }

        return notificationService;
    }

    // Returns the strategy only if it supports the expected DTO type.
    public NotificationService getTypedNotificationStrategy(
            NotificationTypes notificationType) {

       return getNotificationStrategy(notificationType);

    }

}
