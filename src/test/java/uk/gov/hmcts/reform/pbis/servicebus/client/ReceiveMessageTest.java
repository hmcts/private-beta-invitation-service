package uk.gov.hmcts.reform.pbis.servicebus.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.microsoft.azure.servicebus.IMessage;
import org.junit.Test;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusException;


public class ReceiveMessageTest extends AbstractServiceBusClientTest {

    @Test
    public void should_call_receiver() throws Exception {
        IMessage expectedMessage = mock(IMessage.class);
        given(messageReceiver.receive(any())).willReturn(expectedMessage);

        assertSame(expectedMessage, client.receiveMessage());
        verify(messageReceiver).receive(MAX_RECEIVE_TIME);
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void should_fail_when_receiver_fails() throws Exception {
        Exception expectedCause =
            new com.microsoft.azure.servicebus.primitives.ServiceBusException(true);

        given(messageReceiver.receive(any())).willThrow(expectedCause);

        assertThatThrownBy(() -> client.receiveMessage())
            .isInstanceOf(ServiceBusException.class)
            .hasMessage("Failed to receive message from subscription")
            .hasCause(expectedCause);
    }
}
