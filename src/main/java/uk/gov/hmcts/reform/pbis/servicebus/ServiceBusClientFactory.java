package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;

import java.time.Duration;


public class ServiceBusClientFactory implements IServiceBusClientFactory {

    private final String connectionString;
    private final Duration maxReceiveWaitTime;

    public ServiceBusClientFactory(String connectionString, Duration maxReceiveWaitTime) {
        this.connectionString = connectionString;
        this.maxReceiveWaitTime = maxReceiveWaitTime;
    }

    public IServiceBusClient createClient() {
        try {
            IMessageReceiver receiver = ClientFactory.createMessageReceiverFromConnectionString(
                connectionString,
                ReceiveMode.PEEKLOCK
            );

            return new ServiceBusClient(receiver, maxReceiveWaitTime);
        } catch (Exception e) {
            throw new ServiceBusException("Failed to create Service Bus client", e);
        }
    }

}
