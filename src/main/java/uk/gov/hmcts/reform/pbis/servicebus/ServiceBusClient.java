package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import java.util.UUID;


public class ServiceBusClient implements IServiceBusClient {

    private final IMessageReceiver messageReceiver;

    public ServiceBusClient(IMessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    @Override
    public IMessage receiveMessage() {
        try {
            return this.messageReceiver.receive();
        } catch (Exception ex) {
            throw new ServiceBusException("Failed to receive message from subscription", ex);
        }
    }

    @Override
    public void completeMessage(String messageId, UUID messageLockToken) {
        try {
            messageReceiver.complete(messageLockToken);
        } catch (Exception e) {
            throw new ServiceBusException(
                String.format("Failed to mark message as completed. Message ID: %s", messageId),
                e
            );
        }
    }

    @Override
    public void close() throws Exception {
        this.messageReceiver.close();
    }
}
