package uk.gov.hmcts.reform.pbis.integration;

import com.microsoft.azure.servicebus.IMessage;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pbis.Configuration;
import uk.gov.hmcts.reform.pbis.servicebus.IServiceBusClient;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusClientFactory;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusException;
import uk.gov.hmcts.reform.pbis.utils.DeadLetterQueueHelper;
import uk.gov.hmcts.reform.pbis.utils.ServiceBusFeeder;


@TestPropertySource(properties =
    {
        // prevent scheduled jobs from running
        "scheduling.enable=false",

        // reduce the amount of time receive operation should take
        // before returning null from an empty subscription
        "serviceBus.maxReceiveWaitTimeInMs=300"
    }
)
public abstract class AbstractServiceBusTest {

    protected static final Configuration testConfig = new Configuration();
    private static final Logger logger = LoggerFactory.getLogger(AbstractServiceBusTest.class);

    @Autowired
    protected ServiceBusClientFactory serviceBusClientFactory;

    // Cache that stores all messages that have been received from the subscription
    // but not deleted. Such messages would reappear later in the subscription,
    // possibly interfering with tests
    protected final Map<String, IMessage> messagesToComplete = new HashMap<>();

    protected IServiceBusClient serviceBusClient;
    protected ServiceBusFeeder serviceBusFeeder;
    protected DeadLetterQueueHelper deadLetterQueueHelper;

    public void setUp() throws Exception {
        messagesToComplete.clear();
        serviceBusClient = serviceBusClientFactory.createClient();
        serviceBusFeeder = new ServiceBusFeeder(
            testConfig.getServiceBusNamespaceConnectionString(),
            testConfig.getServiceBusTopicName()
        );

        deadLetterQueueHelper = createDeadLetterQueueHelper();

        // make sure the subscription is empty
        consumeAllMessagesFromSubscription();

        deadLetterQueueHelper.clearDeadLetterQueue();
    }

    public void tearDown() throws Exception {
        // make sure messages received from the queue will not reappear during subsequent tests
        completeReceivedMessages();
        consumeAllMessagesFromSubscription();
        deadLetterQueueHelper.clearDeadLetterQueue();

        serviceBusClient.close();
        serviceBusFeeder.close();
        deadLetterQueueHelper.close();
    }

    protected IMessage receiveMessage() {
        IMessage message = serviceBusClient.receiveMessage();

        if (message != null) {
            messagesToComplete.put(message.getMessageId(), message);
        }

        return message;
    }

    private void consumeAllMessagesFromSubscription() {
        IMessage message;
        while ((message = serviceBusClient.receiveMessage()) != null) {
            serviceBusClient.completeMessage(message.getMessageId(), message.getLockToken());
        }
    }

    private void completeReceivedMessages() {
        for (IMessage message : messagesToComplete.values()) {
            try {
                serviceBusClient.completeMessage(
                    message.getMessageId(),
                    message.getLockToken()
                );

                logger.info(
                    String.format("Completed test message. ID: %s", message.getMessageId())
                );
            } catch (ServiceBusException e) {
                logger.warn("Failed to complete a test message", e);
                // do nothing - in some cases completing messages is impossible at this point,
                // e.g. when message lock expired
            }
        }
    }

    private DeadLetterQueueHelper createDeadLetterQueueHelper() throws Exception {
        return new DeadLetterQueueHelper(
            testConfig.getServiceBusNamespaceConnectionString(),
            testConfig.getServiceBusTopicName(),
            testConfig.getServiceBusSubscriptionName(),
            testConfig.getMaxReceiveWaitTime()
        );
    }
}
