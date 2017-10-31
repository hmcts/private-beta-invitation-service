package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.IMessage;

import java.util.UUID;

public interface IServiceBusClient extends AutoCloseable {

    IMessage receiveMessage();

    void completeMessage(String messageId, UUID messageLockToken);
}
