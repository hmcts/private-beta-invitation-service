package uk.gov.hmcts.reform.pbis.servicebus.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.UUID;
import org.junit.Test;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusException;


public class CompleteMessageTest extends AbstractServiceBusClientTest {

    @Test
    public void should_call_receiver() throws Exception {
        UUID lockToken = UUID.randomUUID();
        client.completeMessage("message-id-123", lockToken);

        verify(messageReceiver).complete(lockToken);
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void should_fail_when_receiver_fails() throws Exception {
        Exception expectedCause =
            new com.microsoft.azure.servicebus.primitives.ServiceBusException(true);

        willThrow(expectedCause).given(messageReceiver).complete(any());

        String messageId = "message-id-123";

        assertThatThrownBy(() -> client.completeMessage(messageId, UUID.randomUUID()))
            .isInstanceOf(ServiceBusException.class)
            .hasMessage("Failed to mark message as completed. Message ID: " + messageId)
            .hasCause(expectedCause);
    }
}
