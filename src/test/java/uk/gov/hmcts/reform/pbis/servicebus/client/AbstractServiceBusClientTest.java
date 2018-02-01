package uk.gov.hmcts.reform.pbis.servicebus.client;

import com.microsoft.azure.servicebus.IMessageReceiver;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusClient;

import java.time.Duration;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractServiceBusClientTest {

    protected static final Duration MAX_RECEIVE_TIME = Duration.ofMillis(1);

    @Mock
    protected IMessageReceiver messageReceiver;

    protected ServiceBusClient client;

    @Before
    public void setUp() {
        client = new ServiceBusClient(messageReceiver, MAX_RECEIVE_TIME);
    }
}
