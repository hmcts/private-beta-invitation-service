package uk.gov.hmcts.reform.pbis.servicebus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;

import java.time.Duration;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ServiceBusClientTest {

    private static final Duration MAX_RECEIVE_TIME = Duration.ofMillis(1);

    @Mock
    private IMessageReceiver messageReceiver;

    private ServiceBusClient client;

    @Before
    public void setUp() {
        client = new ServiceBusClient(messageReceiver, MAX_RECEIVE_TIME);
    }

    @Test
    public void receiveMessage_should_call_receiver() throws Exception {
        IMessage expectedMessage = mock(IMessage.class);
        given(messageReceiver.receive(any())).willReturn(expectedMessage);

        assertSame(expectedMessage, client.receiveMessage());
        verify(messageReceiver).receive(MAX_RECEIVE_TIME);
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void receiveMessage_should_fail_when_receiver_fails() throws Exception {
        Exception expectedCause =
            new com.microsoft.azure.servicebus.primitives.ServiceBusException(true);

        given(messageReceiver.receive(any())).willThrow(expectedCause);

        assertThatThrownBy(() -> client.receiveMessage())
            .isInstanceOf(ServiceBusException.class)
            .hasMessage("Failed to receive message from subscription")
            .hasCause(expectedCause);
    }

    @Test
    public void completeMessage_should_call_receiver() throws Exception {
        UUID lockToken = UUID.randomUUID();
        client.completeMessage("message-id-123", lockToken);

        verify(messageReceiver).complete(lockToken);
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void completeMessage_should_fail_when_receiver_fails() throws Exception {
        Exception expectedCause =
            new com.microsoft.azure.servicebus.primitives.ServiceBusException(true);

        willThrow(expectedCause).given(messageReceiver).complete(any());

        String messageId = "message-id-123";

        assertThatThrownBy(() -> client.completeMessage(messageId, UUID.randomUUID()))
            .isInstanceOf(ServiceBusException.class)
            .hasMessage("Failed to mark message as completed. Message ID: " + messageId)
            .hasCause(expectedCause);
    }

    @Test
    public void close_should_close_the_receiver() throws Exception {
        client.close();

        verify(messageReceiver).close();
    }

    @Test
    public void close_should_fail_when_receiver_fails_to_close() throws Exception {
        Exception expectedCause = new RuntimeException("test exception");

        willThrow(expectedCause).given(messageReceiver).close();

        assertThatThrownBy(() -> client.close()).isSameAs(expectedCause);
    }
}
