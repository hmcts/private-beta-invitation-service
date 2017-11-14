package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.IMessage;

import java.util.Map;
import java.util.UUID;

public interface IServiceBusClient extends AutoCloseable {

    IMessage receiveMessage();

    void completeMessage(String messageId, UUID messageLockToken);

    void sendToDeadLetter(
        IMessage message,
        String reason,
        String description,
        Map<String, String> fieldValidationErrors
    );
}
