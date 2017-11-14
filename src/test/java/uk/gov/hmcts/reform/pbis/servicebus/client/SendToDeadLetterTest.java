package uk.gov.hmcts.reform.pbis.servicebus.client;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.microsoft.azure.servicebus.IMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusException;


public class SendToDeadLetterTest extends AbstractServiceBusClientTest {

    @Test
    public void should_call_receiver_with_right_arguments() throws Exception {
        String messageId = "messageId123";
        UUID lockToken = UUID.randomUUID();
        String reason = "test reason";
        String description = "test description";

        Map<String, String> fieldValidationErrors = new HashMap<>();
        fieldValidationErrors.put("field1", "error1");
        fieldValidationErrors.put("field2", "error2");

        IMessage message = createMessage(messageId, lockToken);

        client.sendToDeadLetter(message, reason, description, fieldValidationErrors);

        verify(messageReceiver).deadLetter(
            message.getLockToken(),
            reason,
            description,
            singletonMap("validationErrors", "{\"field1\":\"error1\",\"field2\":\"error2\"}")
        );

        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void should_fail_when_receiver_fails() throws Exception {
        String messageId = "message id 123";
        IMessage message = createMessage(messageId, UUID.randomUUID());

        Exception receiverException = new RuntimeException("test exception", null);

        willThrow(receiverException).given(messageReceiver).deadLetter(any(), any(), any(), any());

        assertThatThrownBy(() ->
            client.sendToDeadLetter(message, "reason", "description", null)
        ).isInstanceOf(ServiceBusException.class)
            .hasCause(receiverException)
            .hasMessage("Failed to send message to dead letter queue. Message ID: " + messageId);
    }

    private IMessage createMessage(String messageId, UUID lockToken) {
        IMessage message = mock(IMessage.class);
        given(message.getMessageId()).willReturn(messageId);
        given(message.getLockToken()).willReturn(lockToken);

        return message;
    }
}
