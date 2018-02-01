package uk.gov.hmcts.reform.pbis.utils;

import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

public class NotificationHelper {

    private final NotificationClient notificationClient;


    public NotificationHelper(String notifyApiKey) {
        notificationClient = new NotificationClient(notifyApiKey);
    }

    public List<Notification> getSentEmails(String reference) {
        try {
            return notificationClient.getNotifications(
                null,
                null,
                reference,
                null
            ).getNotifications();
        } catch (NotificationClientException e) {
            throw new RuntimeException("Failed to retrieve emails", e);
        }
    }
}
