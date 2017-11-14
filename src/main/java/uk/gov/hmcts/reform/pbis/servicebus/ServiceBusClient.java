package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class ServiceBusClient implements IServiceBusClient {

    private final IMessageReceiver messageReceiver;
    private final Duration maxReceiveWaitTime;

    public ServiceBusClient(IMessageReceiver messageReceiver, Duration maxReceiveWaitTime) {
        this.messageReceiver = messageReceiver;
        this.maxReceiveWaitTime = maxReceiveWaitTime;
    }

    @Override
    public IMessage receiveMessage() {
        try {
            return this.messageReceiver.receive(maxReceiveWaitTime);
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
    public void sendToDeadLetter(
        IMessage message,
        String reason,
        String description,
        Map<String, String> fieldValidationErrors
    ) {
        try {
            messageReceiver.deadLetter(
                message.getLockToken(),
                reason,
                description,
                toMapOfObjects(fieldValidationErrors)
            );
        } catch (Exception e) {
            String errorMessage = String.format(
                "Failed to send message to dead letter queue. Message ID: %s",
                message.getMessageId()
            );

            throw new ServiceBusException(errorMessage, e);
        }
    }

    @Override
    public void close() throws Exception {
        this.messageReceiver.close();
    }

    private Map<String, Object> toMapOfObjects(
        Map<String, String> stringMap
    ) {
        if (stringMap != null) {
            return stringMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return null;
        }
    }
}
