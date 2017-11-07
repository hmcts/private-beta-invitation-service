package uk.gov.hmcts.reform.pbis;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.microsoft.azure.servicebus.IMessage;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.pbis.categories.IntegrationTests;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.servicebus.IServiceBusClient;
import uk.gov.hmcts.reform.pbis.servicebus.IServiceBusClientFactory;
import uk.gov.hmcts.reform.pbis.servicebus.MessageQueueProcessor;
import uk.gov.hmcts.reform.pbis.utils.SampleData;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Category(IntegrationTests.class)
public class MessageQueueProcessorTest extends AbstractServiceBusTest {

    @Mock
    private EmailService emailService;

    private MessageQueueProcessor messageQueueProcessor;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // using the spy here in order to keep track of received messages
        IServiceBusClientFactory clientFactorySpy = prepareServiceBusClientFactorySpy();
        messageQueueProcessor = new MessageQueueProcessor(clientFactorySpy, emailService);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void run_should_read_all_messages_from_subscription() throws Exception {
        List<PrivateBetaRegistration> registrations = Stream
            .generate(() -> SampleData.getSampleRegistration(testConfig.getServiceName()))
            .limit(5)
            .collect(toList());

        for (PrivateBetaRegistration registration : registrations) {
            serviceBusFeeder.sendMessage(registration);
        }

        messageQueueProcessor.run();

        // check if there's nothing left to read from the subscription
        assertThat(receiveMessage()).as("check if subscription is empty").isNull();
    }

    @Test
    public void run_should_call_email_service_for_each_valid_message() throws Exception {
        int validMessageCount = 3;

        List<PrivateBetaRegistration> registrationsToProcess =
            SampleData.getSampleRegistrations(testConfig.getServiceName(), validMessageCount);

        serviceBusFeeder.sendMessage(registrationsToProcess.get(0));
        serviceBusFeeder.sendMessage("invalid1");
        serviceBusFeeder.sendMessage(registrationsToProcess.get(1));
        serviceBusFeeder.sendMessage("invalid2");
        serviceBusFeeder.sendMessage(registrationsToProcess.get(2));

        messageQueueProcessor.run();

        ArgumentCaptor<PrivateBetaRegistration> registrationCaptor =
            ArgumentCaptor.forClass(PrivateBetaRegistration.class);

        verify(emailService, times(validMessageCount))
            .sendWelcomeEmail(registrationCaptor.capture());

        String[] actualReferenceIds = getRegistrationIds(registrationCaptor.getAllValues());
        String[] expectedReferenceIds = getRegistrationIds(registrationsToProcess);

        assertThat(actualReferenceIds)
            .as("check reference IDs of processed messages")
            .containsExactlyInAnyOrder(expectedReferenceIds);
    }

    @Test
    public void run_should_not_call_email_service_for_invalid_messages() throws Exception {
        serviceBusFeeder.sendMessages("invalid1", "invalid2", "invalid3");

        messageQueueProcessor.run();

        verify(emailService, never()).sendWelcomeEmail(any());
    }

    @Test
    public void run_should_consume_valid_message_so_they_are_not_reprocessed() throws Exception {
        final String invalidMessageContent = "Invalid content";

        serviceBusFeeder.sendMessage(
            SampleData.getSampleRegistration(testConfig.getServiceName())
        );

        serviceBusFeeder.sendMessage(invalidMessageContent);

        messageQueueProcessor.run();

        assertThat(receiveMessage()).as("check if subscription is empty").isNull();

        // wait for message lock to expire
        Thread.sleep(testConfig.getMessageLockTimeoutInMs());

        IMessage remainingMessage = receiveMessage();
        assertThat(remainingMessage).as("check if non-null message was received").isNotNull();
        assertThat(new String(remainingMessage.getBody())).isEqualTo(invalidMessageContent);

        // make sure that the valid message is not in the queue
        assertThat(receiveMessage()).as("check if subscription is empty").isNull();
    }

    private String[] getRegistrationIds(List<PrivateBetaRegistration> registrations) {
        List<String> ids = registrations
            .stream()
            .map(r -> r.referenceId)
            .collect(toList());

        return ids.toArray(new String[ids.size()]);
    }

    /**
     * Prepares a Service Bus client factory spy
     *
     * The spy, instead of an actual client, creates a spy that serves as a proxy
     * to the client. This is required so that we can keep track of received messages
     * and clean them up after tests.
     */
    private IServiceBusClientFactory prepareServiceBusClientFactorySpy() {
        IServiceBusClientFactory clientFactorySpy = spy(serviceBusClientFactory);

        doAnswer(invocation -> prepareServiceBusClientSpy())
            .when(clientFactorySpy)
            .createClient();

        return clientFactorySpy;
    }

    /**
     * Prepares a Service Bus client spy
     *
     * The spy calls the actual client, but also keeps track of messages that
     * have been received and not deleted. This allows for deleting those messages
     * after the test. Otherwise messages created in one test could affect
     * results of another test if their lock expires and they reappear
     * in the subscription.
     */
    private IServiceBusClient prepareServiceBusClientSpy() throws Exception {
        IServiceBusClient clientSpy = spy(serviceBusClient);

        doAnswer(invocation -> receiveMessage()).when(clientSpy).receiveMessage();

        doAnswer(invocation -> {
            String messageId = invocation.getArgumentAt(0, String.class);
            UUID lockToken = invocation.getArgumentAt(1, UUID.class);
            serviceBusClient.completeMessage(messageId, lockToken);
            messagesToComplete.remove(messageId);
            return null;
        })
            .when(clientSpy)
            .completeMessage(any(), any());

        doNothing().when(clientSpy).close();

        return clientSpy;
    }
}
