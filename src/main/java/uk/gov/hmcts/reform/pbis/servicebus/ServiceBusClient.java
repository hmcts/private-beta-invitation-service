package uk.gov.hmcts.reform.pbis.servicebus;

import static java.util.Collections.singletonMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;


public class ServiceBusClient implements IServiceBusClient {

    private static final String VALIDATION_ERRORS_PROPERTY_KEY = "validationErrors";
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
                getPropertiesToChange(fieldValidationErrors)
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

    private Map<String, Object> getPropertiesToChange(
        Map<String, String> fieldValidationErrors
    ) throws JsonProcessingException {

        if (fieldValidationErrors != null) {
            return singletonMap(
                VALIDATION_ERRORS_PROPERTY_KEY,
                objectMapper.writeValueAsString(fieldValidationErrors)
            );
        } else {
            return null;
        }
    }
}
