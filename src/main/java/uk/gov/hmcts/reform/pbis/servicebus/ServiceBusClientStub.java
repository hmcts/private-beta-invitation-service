package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.azure.servicebus.IMessage;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;


public class ServiceBusClientStub implements IServiceBusClient {

    private static final ServiceBusClientStub instance = new ServiceBusClientStub();
    private Queue<IMessage> messagesToReceive = new LinkedList<>();

    private ServiceBusClientStub() {
        // hiding default constructor
    }

    public static ServiceBusClientStub getInstance() {
        return instance;
    }

    @Override
    public IMessage receiveMessage() {
        return messagesToReceive.poll();
    }

    @Override
    public void completeMessage(String messageId, UUID messageLockToken) {
        // nothing to be done
    }

    @Override
    public void close() throws Exception {
        // nothing to be done
    }

    public void setMessagesToReceive(Queue<IMessage> messagesToReceive) {
        this.messagesToReceive = messagesToReceive;
    }
}
