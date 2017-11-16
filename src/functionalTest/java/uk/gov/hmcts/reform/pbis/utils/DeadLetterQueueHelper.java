package uk.gov.hmcts.reform.pbis.utils;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeadLetterQueueHelper implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHelper.class);
    private final IMessageReceiver messageReceiver;
    private final Duration maxReceiveWaitTime;

    public DeadLetterQueueHelper(
        String namespaceConnectionString,
        String topicName,
        String subscriptionName,
        Duration maxReceiveWaitTime) throws ServiceBusException, InterruptedException {

        this.messageReceiver = buildMessageReceiver(
            namespaceConnectionString,
            topicName,
            subscriptionName
        );

        this.maxReceiveWaitTime = maxReceiveWaitTime;
    }

    public IMessage receiveMessage() throws ServiceBusException, InterruptedException {
        logger.info("Receiving message...");
        IMessage message = messageReceiver.receive(maxReceiveWaitTime);

        if (message != null) {
            logger.info("Received message with content: {}", new String(message.getBody()));
        } else {
            logger.info("Received no message");
        }

        return message;
    }

    public void clearDeadLetterQueue() throws ServiceBusException, InterruptedException {
        logger.info("Clearing dead letter queue...");
        int deletedMessagesCount = 0;

        IMessage message;
        while ((message = receiveMessage()) != null) {
            messageReceiver.complete(message.getLockToken());
            deletedMessagesCount++;
        }

        logger.info(
            "Finished clearing dead letter queue. Deleted messages: {}",
            deletedMessagesCount
        );
    }

    @Override
    public void close() throws Exception {
        messageReceiver.close();
    }

    private IMessageReceiver buildMessageReceiver(
        String namespaceConnectionString,
        String topicName,
        String subscriptionName
    ) throws InterruptedException, ServiceBusException {

        String deadLetterQueuePath = String.format(
            "%s/subscriptions/%s/$DeadLetterQueue",
            topicName,
            subscriptionName
        );

        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(
            namespaceConnectionString,
            deadLetterQueuePath
        );

        return ClientFactory.createMessageReceiverFromConnectionStringBuilder(
            connectionStringBuilder,
            ReceiveMode.PEEKLOCK
        );
    }
}
