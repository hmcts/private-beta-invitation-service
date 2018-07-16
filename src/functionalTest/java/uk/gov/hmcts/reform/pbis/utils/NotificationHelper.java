package uk.gov.hmcts.reform.pbis.utils;

import org.awaitility.Duration;
import uk.gov.hmcts.reform.pbis.Configuration;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;

public class NotificationHelper {

    private final NotificationClient notificationClient;
    private static final Configuration config = new Configuration();

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

    public void waitForNotificationToBeConsumed(String referenceId) {
        await()
            .atMost(new Duration(config.getServiceBusPollingDelayInMs(), MILLISECONDS).plus(FIVE_SECONDS))
            .pollDelay(500, MILLISECONDS)
            .until(() -> !getSentEmails(referenceId).isEmpty());
    }
}
