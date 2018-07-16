package uk.gov.hmcts.reform.pbis.e2e;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.gov.hmcts.reform.pbis.Configuration;
import uk.gov.hmcts.reform.pbis.categories.FunctionalTests;
import uk.gov.hmcts.reform.pbis.servicebus.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.utils.DeadLetterQueueHelper;
import uk.gov.hmcts.reform.pbis.utils.NotificationHelper;
import uk.gov.hmcts.reform.pbis.utils.ServiceBusFeeder;
import uk.gov.service.notify.Notification;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pbis.utils.SampleData.getSampleInvalidRegistration;
import static uk.gov.hmcts.reform.pbis.utils.SampleData.getSampleRegistration;

@Category(FunctionalTests.class)
public class FunctionalTest {

    private static final Configuration config = new Configuration();

    private ServiceBusFeeder serviceBusFeeder;
    private NotificationHelper notificationHelper;
    private DeadLetterQueueHelper deadLetterQueueHelper;

    @Before
    public void setUp() throws ServiceBusException, InterruptedException {
        serviceBusFeeder = new ServiceBusFeeder(
            config.getServiceBusNamespaceConnectionString(),
            config.getServiceBusTopicName()
        );

        deadLetterQueueHelper = new DeadLetterQueueHelper(
            config.getServiceBusNamespaceConnectionString(),
            config.getServiceBusTopicName(),
            config.getServiceBusSubscriptionName(),
            config.getMaxReceiveWaitTime()
        );

        notificationHelper = new NotificationHelper(config.getNotifyApiKey());
    }

    @After
    public void tearDown() throws Exception {
        deadLetterQueueHelper.clearDeadLetterQueue();

        deadLetterQueueHelper.close();
        serviceBusFeeder.close();
    }

    @Test
    public void should_send_welcome_email_when_valid_message_present() throws Exception {

        PrivateBetaRegistration registration = getSampleRegistration(config.getServiceName());

        serviceBusFeeder.sendMessage(registration);

        notificationHelper.waitForNotificationToBeConsumed(registration.referenceId);

        List<Notification> emails = notificationHelper.getSentEmails(registration.referenceId);

        assertThat(emails).hasSize(1);
        Notification email = emails.get(0);

        assertThat(email).isNotNull();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(email.getEmailAddress())
            .isEqualTo(Optional.of(registration.emailAddress));

        softly.assertThat(email.getNotificationType()).isEqualTo("email");
        softly.assertThat(email.getTemplateId().toString()).isEqualTo(config.getTemplateId());

        // check email template was filled with appropriate values
        softly.assertThat(email.getBody()).contains(registration.firstName);
        softly.assertThat(email.getBody()).contains(registration.lastName);
        softly.assertThat(email.getBody()).contains(config.getWelcomeLink());

        softly.assertAll();
    }

    @Test
    public void should_not_send_email_when_message_is_invalid() throws Exception {
        //given
        String invalidQueueMessage = "invalid format";
        PrivateBetaRegistration invalidRegistration = getSampleInvalidRegistration();
        PrivateBetaRegistration validRegistration = getSampleRegistration(config.getServiceName());

        // when
        serviceBusFeeder.sendMessage(invalidQueueMessage);
        serviceBusFeeder.sendMessage(invalidRegistration);
        serviceBusFeeder.sendMessage(validRegistration);

        notificationHelper.waitForNotificationToBeConsumed(validRegistration.referenceId);

        List<Notification> invalidInvitationEmails = notificationHelper.getSentEmails(invalidRegistration.referenceId);

        // then
        assertThat(invalidInvitationEmails).isEmpty();
        assertTwoMessagesInDeadLetterQueue();
    }

    @Test
    public void should_not_send_email_when_message_references_unknown_service() throws Exception {
        // given
        PrivateBetaRegistration unknownService = getSampleRegistration("unknown-service-123");
        PrivateBetaRegistration knownService = getSampleRegistration(config.getServiceName());

        // when
        serviceBusFeeder.sendMessage(unknownService);
        serviceBusFeeder.sendMessage(knownService);

        // and
        notificationHelper.waitForNotificationToBeConsumed(knownService.referenceId);
        List<Notification> unknownServiceEmails = notificationHelper.getSentEmails(unknownService.referenceId);

        // then
        assertThat(unknownServiceEmails).isEmpty();
    }

    @Test
    public void should_send_multiple_emails_when_multiple_messages_are_present() throws Exception {
        int numberOfMessages = 5;

        List<PrivateBetaRegistration> registrations =
            Stream.generate(() -> getSampleRegistration(config.getServiceName()))
                .limit(numberOfMessages)
                .collect(toList());

        for (PrivateBetaRegistration registration : registrations) {
            serviceBusFeeder.sendMessage(registration);
        }

        registrations.forEach(r -> notificationHelper.waitForNotificationToBeConsumed(r.referenceId));

        List<String> referenceIds = registrations.stream()
            .map(r -> r.referenceId)
            .collect(toList());

        assertThat(referenceIds).allMatch(
            referenceId -> notificationHelper.getSentEmails(referenceId).size() == 1
        );
    }

    private void assertTwoMessagesInDeadLetterQueue() throws Exception {
        assertThat(deadLetterQueueHelper.receiveMessage())
            .as("first message in dead letter queue")
            .isNotNull();

        assertThat(deadLetterQueueHelper.receiveMessage())
            .as("second message in dead letter queue")
            .isNotNull();

        assertThat(deadLetterQueueHelper.receiveMessage())
            .as("check if dead letter queue is empty")
            .isNull();
    }
}
