package uk.gov.hmcts.reform.pbis.servicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.microsoft.azure.servicebus.IMessage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;


public class ServiceBusClientStubTest {

    private ServiceBusClientStub clientStub = ServiceBusClientStub.getInstance();


    @Test
    public void receiveMessage_should_return_null_when_queue_is_empty() {
        clientStub.setMessagesToReceive(new LinkedList<>());
        assertThat(clientStub.receiveMessage()).isNull();
    }

    @Test
    public void receiveMessage_should_return_messages_from_queue_until_empty() {
        List<IMessage> messages = Arrays.asList(
            mock(IMessage.class),
            mock(IMessage.class)
        );

        clientStub.setMessagesToReceive(new LinkedList(messages));

        assertThat(clientStub.receiveMessage()).isSameAs(messages.get(0));
        assertThat(clientStub.receiveMessage()).isSameAs(messages.get(1));
        assertThat(clientStub.receiveMessage()).isNull();
    }

    @Test
    public void setMessagesToReceive_should_rewrite_message_queue() {
        List<IMessage> nonEmptyList = Arrays.asList(mock(IMessage.class));

        clientStub.setMessagesToReceive(new LinkedList<>(nonEmptyList));
        assertThat(clientStub.receiveMessage()).isNotNull();

        clientStub.setMessagesToReceive(new LinkedList<>());
        assertThat(clientStub.receiveMessage()).isNull();

        clientStub.setMessagesToReceive(new LinkedList<>(nonEmptyList));
        assertThat(clientStub.receiveMessage()).isNotNull();
    }

    @Test
    public void completeMessage_should_not_throw_exception() {
        assertThatCode(() -> {
            clientStub.completeMessage("message-id-123", UUID.randomUUID());
        }).doesNotThrowAnyException();
    }

    @Test
    public void close_should_not_throw_exception() throws Exception {
        clientStub.close();
    }

}
