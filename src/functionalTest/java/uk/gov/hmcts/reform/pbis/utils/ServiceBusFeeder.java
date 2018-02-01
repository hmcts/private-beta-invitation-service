package uk.gov.hmcts.reform.pbis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.ITopicClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

import java.util.UUID;

public class ServiceBusFeeder implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusFeeder.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ITopicClient topicClient;

    public ServiceBusFeeder(
        String namespaceConnectionString,
        String subscriptionPath
    ) throws ServiceBusException, InterruptedException {

        topicClient = new TopicClient(
            new ConnectionStringBuilder(namespaceConnectionString, subscriptionPath)
        );
    }

    public void sendMessages(
        String... messageContents
    ) throws ServiceBusException, InterruptedException {

        for (String content: messageContents) {
            sendMessage(content);
        }
    }

    public IMessage sendMessage(
        PrivateBetaRegistration registration
    ) throws JsonProcessingException, ServiceBusException, InterruptedException {

        logger.info(
            String.format("Sending registration with referenceId: %s", registration.referenceId)
        );

        String messageContent = mapper.writeValueAsString(registration);
        IMessage message = sendMessage(messageContent);

        logger.info(
            String.format("Registration sent. Reference Id: %s", registration.referenceId)
        );

        return message;
    }

    public IMessage sendMessage(String content) throws ServiceBusException, InterruptedException {
        logger.info(String.format("Sending message with content: %s", content));
        String messageId = "test-message-" + UUID.randomUUID().toString();
        IMessage message = new Message(messageId, content, null);
        topicClient.send(message);
        logger.info(String.format("Sent message with content: %s", content));
        return message;
    }

    @Override
    public void close() throws Exception {
        topicClient.close();
    }
}
