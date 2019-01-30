package uk.gov.hmcts.reform.pbis.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.servicebus.IMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.pbis.EmailService;
import uk.gov.hmcts.reform.pbis.ServiceNotFoundException;
import uk.gov.hmcts.reform.pbis.categories.IntegrationTests;
import uk.gov.hmcts.reform.pbis.servicebus.IServiceBusClient;
import uk.gov.hmcts.reform.pbis.servicebus.IServiceBusClientFactory;
import uk.gov.hmcts.reform.pbis.servicebus.MessageQueueProcessingTracker;
import uk.gov.hmcts.reform.pbis.servicebus.MessageQueueProcessor;
import uk.gov.hmcts.reform.pbis.servicebus.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.utils.SampleData;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.validation.Validation;
import javax.validation.Validator;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Category(IntegrationTests.class)
public class MessageQueueProcessorTest extends AbstractServiceBusTest {

    private static final String DEAD_LETTER_REASON_KEY = "DeadLetterReason";
    private static final String DEAD_LETTER_DESCRIPTION_KEY = "DeadLetterErrorDescription";
    private static final String VALIDATION_ERRORS_KEY = "ValidationErrors";

    @Mock
    private EmailService emailService;

    @Mock
    private MessageQueueProcessingTracker tracker;

    private Validator validator;

    private MessageQueueProcessor messageQueueProcessor;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // using the spy here in order to keep track of received messages
        IServiceBusClientFactory clientFactorySpy = prepareServiceBusClientFactorySpy();

        validator = Validation.buildDefaultValidatorFactory().getValidator();

        messageQueueProcessor = new MessageQueueProcessor(
            clientFactorySpy,
            emailService,
            validator,
            tracker
        );
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

        // make sure that the valid message is not in the queue
        assertThat(receiveMessage()).as("check if subscription is empty").isNull();
    }

    @Test
    public void run_should_send_malformed_messages_to_dead_letter() throws Exception {
        IMessage originalMessage = serviceBusFeeder.sendMessage("Invalid content");

        messageQueueProcessor.run();

        assertThat(receiveMessage()).as("check if subscription is empty").isNull();

        IMessage deadLetterMessage = deadLetterQueueHelper.receiveMessage();
        assertDeadLetterMessageIsForMalformedContent(deadLetterMessage, originalMessage);

        assertThat(deadLetterQueueHelper.receiveMessage())
            .as("check if dead letter queue is empty")
            .isNull();
    }

    @Test
    public void run_should_send_well_formed_invalid_messages_to_dead_letter() throws Exception {
        PrivateBetaRegistration invalidRegistration = getRegistrationWithInvalidEmail();
        serviceBusFeeder.sendMessage(invalidRegistration);

        messageQueueProcessor.run();

        assertThat(receiveMessage()).as("check if subscription is empty").isNull();

        IMessage deadLetterMessage = deadLetterQueueHelper.receiveMessage();

        assertDeadLetterMessageIsForInvalidRegistration(
            deadLetterMessage,
            invalidRegistration,
            "{\"emailAddress\":\"not a well-formed email address\"}");

        assertThat(deadLetterQueueHelper.receiveMessage())
            .as("check if dead letter queue is empty")
            .isNull();
    }

    @Test
    public void run_should_send_messages_with_unknown_service_to_dead_letter() throws Exception {
        final IMessage originalMessage = serviceBusFeeder.sendMessage(
            SampleData.getSampleRegistration("unknown-service")
        );

        willThrow(new ServiceNotFoundException("test"))
            .given(emailService)
            .sendWelcomeEmail(any());

        messageQueueProcessor.run();

        assertThat(receiveMessage()).as("check if subscription is empty").isNull();

        IMessage deadLetterMessage = deadLetterQueueHelper.receiveMessage();

        assertDeadLetterMessageIsForUnknownService(deadLetterMessage, originalMessage);

        assertThat(deadLetterQueueHelper.receiveMessage())
            .as("check if dead letter queue is empty")
            .isNull();
    }

    private void assertDeadLetterMessageIsForUnknownService(
        IMessage deadLetterMessage,
        IMessage originalMessage
    ) {
        assertThat(deadLetterMessage).as("dead letter message").isNotNull();

        assertThat(bodyAsString(deadLetterMessage))
            .as("dead letter message body")
            .isEqualTo(bodyAsString(originalMessage));

        assertThat(deadLetterMessage.getMessageId())
            .as("dead letter message ID")
            .isEqualTo(originalMessage.getMessageId());

        assertThat(deadLetterMessage.getProperties())
            .as("dead letter message properties")
            .isEqualTo(
                ImmutableMap.of(
                    DEAD_LETTER_REASON_KEY, "Unknown service",
                    DEAD_LETTER_DESCRIPTION_KEY, "The message references an unknown service"
                )
            );
    }

    private void assertDeadLetterMessageIsForInvalidRegistration(
        IMessage deadLetterMessage,
        PrivateBetaRegistration invalidRegistration,
        String validationErrorsString
    ) throws JsonProcessingException {

        assertThat(deadLetterMessage).as("check if dead letter message is present").isNotNull();

        assertThat(bodyAsString(deadLetterMessage))
            .as("dead letter message body")
            .isEqualTo(objectMapper.writeValueAsString(invalidRegistration));

        Map<String, String> expectedProperties = ImmutableMap.of(
            DEAD_LETTER_REASON_KEY, "Invalid message",
            DEAD_LETTER_DESCRIPTION_KEY, "Message contains invalid data",
            VALIDATION_ERRORS_KEY,  validationErrorsString
        );

        assertThat(deadLetterMessage.getProperties())
            .as("dead letter message properties")
            .isEqualTo(expectedProperties);
    }

    private void assertDeadLetterMessageIsForMalformedContent(
        IMessage deadLetterMessage,
        IMessage originalMessage
    ) {
        assertThat(deadLetterMessage).as("check if dead letter message is present").isNotNull();
        assertThat(bodyAsString(deadLetterMessage)).isEqualTo(bodyAsString(originalMessage));

        assertThat(deadLetterMessage.getMessageId())
            .as("dead letter message ID")
            .isEqualTo(originalMessage.getMessageId());

        Map<String, String> expectedProperties = ImmutableMap.of(
            DEAD_LETTER_REASON_KEY, "Invalid message",
            DEAD_LETTER_DESCRIPTION_KEY, "Message body has invalid format"
        );

        assertThat(deadLetterMessage.getProperties())
            .as("dead letter message properties")
            .isEqualTo(expectedProperties);
    }

    private String[] getRegistrationIds(List<PrivateBetaRegistration> registrations) {
        List<String> ids = registrations
            .stream()
            .map(r -> r.referenceId)
            .collect(toList());

        return ids.toArray(new String[ids.size()]);
    }

    /**
     * Prepares a Service Bus client factory spy.
     *
     * <p>The spy, instead of an actual client, creates a spy that serves as a proxy
     * to the client. This is required so that we can keep track of received messages
     * and clean them up after tests.</p>
     */
    private IServiceBusClientFactory prepareServiceBusClientFactorySpy() {
        IServiceBusClientFactory clientFactorySpy = spy(serviceBusClientFactory);

        doAnswer(invocation -> prepareServiceBusClientSpy())
            .when(clientFactorySpy)
            .createClient();

        return clientFactorySpy;
    }

    /**
     * Prepares a Service Bus client spy.
     *
     * <p>The spy calls the actual client, but also keeps track of messages that
     * have been received and not deleted. This allows for deleting those messages
     * after the test. Otherwise messages created in one test could affect
     * results of another test if their lock expires and they reappear
     * in the subscription.</p>
     */
    private IServiceBusClient prepareServiceBusClientSpy() throws Exception {
        IServiceBusClient clientSpy = spy(serviceBusClient);

        doAnswer(invocation -> receiveMessage()).when(clientSpy).receiveMessage();

        doAnswer(invocation -> {
            String messageId = invocation.getArgument(0);
            UUID lockToken = invocation.getArgument(1);
            serviceBusClient.completeMessage(messageId, lockToken);
            messagesToComplete.remove(messageId);
            return null;
        })
            .when(clientSpy)
            .completeMessage(any(), any());

        doNothing().when(clientSpy).close();

        return clientSpy;
    }

    private PrivateBetaRegistration getRegistrationWithInvalidEmail() {
        return new PrivateBetaRegistration(
            "valid reference id",
            testConfig.getServiceName(),
            "not-a-valid-email",
            "John",
            "Smith"
        );
    }
}
