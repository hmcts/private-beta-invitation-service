package uk.gov.hmcts.reform.pbis.e2e;

import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.gov.hmcts.reform.pbis.Configuration;
import uk.gov.hmcts.reform.pbis.categories.SmokeTests;
import uk.gov.hmcts.reform.pbis.servicebus.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.utils.NotificationHelper;
import uk.gov.hmcts.reform.pbis.utils.ServiceBusFeeder;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static uk.gov.hmcts.reform.pbis.utils.SampleData.getSampleRegistration;

@Category(SmokeTests.class)
public class SmokeTest {

    private static final Configuration config = new Configuration();

    private ServiceBusFeeder serviceBusFeeder;
    private NotificationHelper notificationHelper;

    @Before
    public void setUp() throws Exception {
        serviceBusFeeder = new ServiceBusFeeder(
            config.getServiceBusNamespaceConnectionString(),
            config.getServiceBusTopicName()
        );

        notificationHelper = new NotificationHelper(config.getNotifyApiKey());
    }

    @Test
    public void should_send_email_when_valid_message_is_put_on_queue() throws Exception {
        // given
        PrivateBetaRegistration validReg = getSampleRegistration(config.getServiceName());

        // when
        serviceBusFeeder.sendMessage(validReg);

        // then
        assertThatCode(() -> waitForNotificationToBeConsumed(validReg.referenceId))
            .as("Notification should be consumed by GOV Notify")
            .doesNotThrowAnyException();
    }

    private void waitForNotificationToBeConsumed(String referenceId) {
        await()
            .atMost(new Duration(config.getServiceBusPollingDelayInMs(), MILLISECONDS).plus(FIVE_SECONDS))
            .pollDelay(500, MILLISECONDS)
            .until(() -> !notificationHelper.getSentEmails(referenceId).isEmpty());
    }

    @After
    public void tearDown() throws Exception {
        serviceBusFeeder.close();
    }
}
