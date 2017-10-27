package uk.gov.hmcts.reform.pbis;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.utils.ServiceBusFeeder;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;


public class SubscriptionProcessingTest {

    private static final Configuration config = new Configuration();

    private static final Duration MAX_WAIT_TIME = new Duration(
        config.getServiceBusPollingDelayInMs() + 2000,
        MILLISECONDS
    );

    private ServiceBusFeeder serviceBusFeeder;
    private NotificationClient notificationClient;

    @Before
    public void setUp() throws ServiceBusException, InterruptedException {
        serviceBusFeeder = new ServiceBusFeeder(config.getServiceBusConnectionString());
        notificationClient = new NotificationClient(config.getNotifyApiKey());
    }

    @Test
    public void should_send_welcome_email_when_valid_message_present() throws Exception {

        PrivateBetaRegistration registration = getSampleRegistration(config.getServiceName());

        serviceBusFeeder.sendMessage(registration);

        waitUntilRegistrationIsProcessed(registration.referenceId);

        List<Notification> emails = getSentEmails(registration.referenceId);

        assertThat(emails).hasSize(1);
        Notification email = emails.get(0);

        assertThat(email).isNotNull();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(email.getEmailAddress())
            .isEqualTo(Optional.of(registration.emailAddress));

        softly.assertThat(email.getNotificationType()).isEqualTo("email");
        softly.assertThat(email.getTemplateId().toString()).isEqualTo(config.getTemplateId());
        softly.assertThat(email.getBody()).contains("First name: " + registration.firstName);
        softly.assertThat(email.getBody()).contains("Last name: " + registration.lastName);
        softly.assertThat(email.getBody()).contains("Welcome link: " + config.getWelcomeLink());
        softly.assertAll();
    }

    @Test
    public void should_not_send_email_when_message_references_unknown_service() throws Exception {
        PrivateBetaRegistration registrationWithUnknownService =
            getSampleRegistration("unknown-service-123");

        serviceBusFeeder.sendMessage(registrationWithUnknownService);

        // sending a valid message to make sure the service has finished processing
        PrivateBetaRegistration registrationWithKnownService =
            getSampleRegistration(config.getServiceName());
        serviceBusFeeder.sendMessage(registrationWithKnownService);
        waitUntilRegistrationIsProcessed(registrationWithKnownService.referenceId);

        List<Notification> sentEmails = getSentEmails(registrationWithUnknownService.referenceId);

        assertThat(sentEmails).isEmpty();
    }

    @Test
    public void should_send_multiple_emails_when_multiple_messages_are_present() throws Exception {
        int numberOfMessages = 5;

        List<PrivateBetaRegistration> registrations =
            Stream.generate(() -> getSampleRegistration(config.getServiceName()))
                .limit(numberOfMessages)
                .collect(Collectors.toList());

        for (PrivateBetaRegistration registration : registrations) {
            serviceBusFeeder.sendMessage(registration);
        }

        // wait for the last sent message to be processed
        waitUntilRegistrationIsProcessed(registrations.get(numberOfMessages - 1).referenceId);

        List<String> referenceIds = registrations.stream()
            .map(r -> r.referenceId)
            .collect(Collectors.toList());

        assertThat(referenceIds).allMatch(referenceId -> getSentEmails(referenceId).size() == 1);
    }

    private void waitUntilRegistrationIsProcessed(String referenceId) {
        await()
            .atMost(MAX_WAIT_TIME)
            .pollDelay(500, MILLISECONDS)
            .until(() -> !getSentEmails(referenceId).isEmpty());
    }

    private List<Notification> getSentEmails(String reference) {
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

    private PrivateBetaRegistration getSampleRegistration(String service) {
        String reference = "pbis-functional-test-" + UUID.randomUUID().toString();

        return new PrivateBetaRegistration(
            reference,
            service,
            "john.smith@example.com",
            "John",
            "Smith"
        );
    }
}
