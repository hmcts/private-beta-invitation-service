package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;


public class ServiceBusClientFactory implements IServiceBusClientFactory {

    private final String connectionString;

    public ServiceBusClientFactory(String connectionString) {
        this.connectionString = connectionString;
    }

    public IServiceBusClient createClient() {
        try {
            IMessageReceiver receiver = ClientFactory.createMessageReceiverFromConnectionString(
                connectionString,
                ReceiveMode.PEEKLOCK
            );

            return new ServiceBusClient(receiver);
        } catch (Exception e) {
            throw new ServiceBusException("Failed to create Service Bus client", e);
        }
    }

}
