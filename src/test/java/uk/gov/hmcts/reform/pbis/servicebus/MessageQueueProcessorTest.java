package uk.gov.hmcts.reform.pbis.servicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import java.util.List;
import java.util.UUID;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pbis.EmailSendingException;
import uk.gov.hmcts.reform.pbis.EmailService;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


@RunWith(MockitoJUnitRunner.class)
public class MessageQueueProcessorTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private IServiceBusClient client;

    @Mock
    private IServiceBusClientFactory clientFactory;

    @Mock
    private EmailService emailService;

    @Mock
    private Validator validator;

    private MessageQueueProcessor messageQueueProcessor;

    @Before
    public void setUp() {
        given(clientFactory.createClient()).willReturn(client);

        // make the mock validator use a real validator for validating (can't spy - final class)
        given(validator.validate(any())).willAnswer(invocation -> {
            PrivateBetaRegistration registration =
                invocation.getArgumentAt(0, PrivateBetaRegistration.class);

            return Validation
                .buildDefaultValidatorFactory()
                .getValidator()
                .validate(registration);
        });

        messageQueueProcessor = new MessageQueueProcessor(clientFactory, emailService, validator);
    }

    @Test
    public void run_should_send_emails_as_long_as_there_are_messages() throws Exception {
        PrivateBetaRegistration registration1 = getValidRegistration();
        PrivateBetaRegistration registration2 = getValidRegistration();

        IMessage message1 = createMessage(registration1);
        IMessage message2 = createMessage(registration2);

        given(client.receiveMessage()).willReturn(message1, message2, null);

        messageQueueProcessor.run();

        ArgumentCaptor<PrivateBetaRegistration> registrationCaptor
            = ArgumentCaptor.forClass(PrivateBetaRegistration.class);

        verify(emailService, times(2)).sendWelcomeEmail(registrationCaptor.capture());
        List<PrivateBetaRegistration> registrations = registrationCaptor.getAllValues();
        assertThat(registrations).hasSize(2);
        assertThat(registrations.get(0)).isEqualToComparingFieldByFieldRecursively(registration1);
        assertThat(registrations.get(1)).isEqualToComparingFieldByFieldRecursively(registration2);
    }

    @Test
    public void run_should_validate_every_well_formed_message() throws Exception {
        PrivateBetaRegistration validRegistration = getValidRegistration();
        PrivateBetaRegistration invalidRegistration = getInvalidRegistration();

        IMessage message1 = createMessage(validRegistration);
        IMessage message2 = new Message("invalid format");
        IMessage message3 = createMessage(invalidRegistration);

        given(client.receiveMessage()).willReturn(message1, message2, message3, null);

        messageQueueProcessor.run();

        ArgumentCaptor<PrivateBetaRegistration> registrationCaptor
            = ArgumentCaptor.forClass(PrivateBetaRegistration.class);

        verify(validator, times(2)).validate(registrationCaptor.capture());
        List<PrivateBetaRegistration> registrations = registrationCaptor.getAllValues();

        assertThat(registrations.get(0))
            .isEqualToComparingFieldByFieldRecursively(validRegistration);

        assertThat(registrations.get(1))
            .isEqualToComparingFieldByFieldRecursively(invalidRegistration);
    }

    @Test
    public void run_should_not_send_emails_for_invalid_messages() throws Exception {
        PrivateBetaRegistration invalidRegistration = getInvalidRegistration();
        PrivateBetaRegistration validRegistration = getValidRegistration();

        IMessage message1 = createMessage(invalidRegistration);
        IMessage message2 = createMessage(validRegistration);
        IMessage message3 = new Message("invalid format");

        given(client.receiveMessage()).willReturn(message1, message2, message3, null);

        messageQueueProcessor.run();

        ArgumentCaptor<PrivateBetaRegistration> registrationCaptor
            = ArgumentCaptor.forClass(PrivateBetaRegistration.class);

        verify(emailService).sendWelcomeEmail(registrationCaptor.capture());

        assertThat(registrationCaptor.getValue()).isNotNull();

        assertThat(registrationCaptor.getValue())
            .as("registration passed to email service")
            .isEqualToComparingFieldByFieldRecursively(validRegistration);

        verifyNoMoreInteractions(emailService);
    }

    @Test
    public void run_should_abort_when_service_bus_client_fails() throws Exception {
        Exception exception = new ServiceBusException("test exception", null);
        given(client.receiveMessage()).willThrow(exception);

        messageQueueProcessor.run();

        verify(client, times(1)).receiveMessage();
        verify(client, never()).completeMessage(any(), any());

        verify(emailService, never()).sendWelcomeEmail(any());
    }

    @Test
    public void run_should_complete_each_successfully_processed_message() throws Exception {
        IMessage message1 = createMessage(getValidRegistration());
        IMessage message2 = createMessage(getValidRegistration());

        given(client.receiveMessage()).willReturn(message1, message2, null);

        messageQueueProcessor.run();

        verify(client, times(3)).receiveMessage();

        verify(client, times(1))
            .completeMessage(message1.getMessageId(), message1.getLockToken());
        verify(client, times(1))
            .completeMessage(message2.getMessageId(), message2.getLockToken());

        verify(client).close();
        verifyNoMoreInteractions(client);
    }

    @Test
    public void run_should_not_complete_message_when_email_service_fails() throws Exception {
        willThrow(new EmailSendingException("test exception", null))
            .given(emailService)
            .sendWelcomeEmail(any());

        IMessage message = createMessage(getValidRegistration());
        given(client.receiveMessage()).willReturn(message, message, null);

        messageQueueProcessor.run();

        verify(client, times(3)).receiveMessage();
        verify(client, never()).completeMessage(any(), any());
    }

    @Test
    public void run_should_continue_processing_when_email_service_fails() throws Exception {
        willThrow(new EmailSendingException("test exception", null))
            .given(emailService)
            .sendWelcomeEmail(any());

        IMessage message = createMessage(getValidRegistration());
        given(client.receiveMessage()).willReturn(message, message, null);

        messageQueueProcessor.run();

        verify(client, times(3)).receiveMessage();
        verify(emailService, times(2)).sendWelcomeEmail(any());
    }

    @Test
    public void run_should_continue_processing_when_message_invalid() {
        IMessage message = new Message("invalid content");
        given(client.receiveMessage()).willReturn(message, message, null);

        messageQueueProcessor.run();

        verify(client, times(3)).receiveMessage();
        verify(emailService, never()).sendWelcomeEmail(any());
    }

    @Test
    public void run_does_not_fail_when_client_factory_throws_exception() {
        given(clientFactory.createClient())
            .willThrow(new RuntimeException("test exception"));

        messageQueueProcessor.run();
    }

    @Test
    public void run_aborts_when_client_throws_exception() throws Exception {
        given(client.receiveMessage())
            .willThrow(new ServiceBusException("test exception", null));

        messageQueueProcessor.run();

        verify(client).receiveMessage();
        verify(client).close();
        verifyNoMoreInteractions(client, emailService);
    }

    private IMessage createMessage(
        PrivateBetaRegistration registration
    ) throws JsonProcessingException {
        IMessage message = mock(IMessage.class);

        UUID lockToken = UUID.randomUUID();
        given(message.getLockToken()).willReturn(lockToken);

        String messageId = "message-" + lockToken.toString();
        given(message.getMessageId()).willReturn(messageId);

        given(message.getBody()).willReturn(objectMapper.writeValueAsBytes(registration));

        return message;
    }

    private PrivateBetaRegistration getValidRegistration() {
        return new PrivateBetaRegistration(
            "reference id " + UUID.randomUUID().toString(),
            "service 123",
            "email@example.com",
            "John",
            "Smith"
        );
    }

    private PrivateBetaRegistration getInvalidRegistration() {
        return new PrivateBetaRegistration(
            "reference id " + UUID.randomUUID().toString(),
            "",
            "not an email",
            "",
            ""
        );
    }
}
