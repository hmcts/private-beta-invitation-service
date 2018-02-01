package uk.gov.hmcts.reform.pbis.servicebus.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

public class CloseTest extends AbstractServiceBusClientTest {

    @Test
    public void should_close_the_receiver() throws Exception {
        client.close();

        verify(messageReceiver).close();
    }

    @Test
    public void should_fail_when_receiver_fails_to_close() throws Exception {
        Exception expectedCause = new RuntimeException("test exception");

        willThrow(expectedCause).given(messageReceiver).close();

        assertThatThrownBy(() -> client.close()).isSameAs(expectedCause);
    }
}
