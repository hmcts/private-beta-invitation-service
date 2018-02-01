package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientFactory.class)
public class ServiceBusClientFactoryTest {

    private static final String CONNECTION_STRING = "connection string";
    private static final Duration MAX_RECEIVE_TIME = Duration.ofMillis(1);

    private ServiceBusClientFactory clientFactory;

    @Before
    public void setUp() throws Exception {
        clientFactory = new ServiceBusClientFactory(CONNECTION_STRING, MAX_RECEIVE_TIME);

        mockStatic(ClientFactory.class);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void createClient_should_create_client_using_receiver_from_factory() throws Exception {

        IMessageReceiver messageReceiver = mock(IMessageReceiver.class);

        when(ClientFactory.createMessageReceiverFromConnectionString(anyString(), any()))
            .thenReturn(messageReceiver);

        IServiceBusClient client = clientFactory.createClient();

        // make sure the returned client uses the receiver
        verify(messageReceiver, never()).receive();
        client.receiveMessage();
        verify(messageReceiver).receive(MAX_RECEIVE_TIME);

        PowerMockito.verifyStatic(ClientFactory.class, times(1));
        ClientFactory.createMessageReceiverFromConnectionString(eq(CONNECTION_STRING), any());
    }

    @Test
    public void createClient_should_return_different_clients_each_time() throws Exception {
        when(ClientFactory.createMessageReceiverFromConnectionString(anyString(), any()))
            .thenReturn(mock(IMessageReceiver.class));

        assertThat(
            clientFactory.createClient()
        ).isNotSameAs(
            clientFactory.createClient()
        );
    }

    @Test
    public void createClient_should_fail_when_client_factory_fails() throws Exception {
        Exception expectedCause =
            new com.microsoft.azure.servicebus.primitives.ServiceBusException(true);

        when(ClientFactory.createMessageReceiverFromConnectionString(anyString(), any()))
            .thenThrow(expectedCause);

        assertThatThrownBy(() -> clientFactory.createClient())
            .isInstanceOf(ServiceBusException.class)
            .hasMessage("Failed to create Service Bus client")
            .hasCause(expectedCause);
    }
}
