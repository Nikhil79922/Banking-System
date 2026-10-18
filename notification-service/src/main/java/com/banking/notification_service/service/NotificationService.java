package com.banking.notification_service.service;

import com.banking.notification_service.dto.NotificationRequest;
import com.banking.notification_service.enums.NotificationTypes;

public interface NotificationService {

    NotificationTypes getNotificationType();

    boolean sendNotification(NotificationRequest request);
}