package uk.gov.hmcts.reform.pbis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.ITopicClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


public class ServiceBusFeeder implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusFeeder.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ITopicClient topicClient;

    public ServiceBusFeeder(
        String connectionString
    ) throws ServiceBusException, InterruptedException {

        topicClient = new TopicClient(new ConnectionStringBuilder(connectionString));
    }

    public void sendMessage(
        PrivateBetaRegistration registration
    ) throws JsonProcessingException, ServiceBusException, InterruptedException {

        logger.info(
            String.format("Sending registration with referenceId: %s", registration.referenceId)
        );

        String messageContent = mapper.writeValueAsString(registration);
        topicClient.send(new Message(messageContent));

        logger.info(
            String.format("Registration sent. Reference Id: %s", registration.referenceId)
        );
    }

    @Override
    public void close() throws Exception {
        topicClient.close();
    }
}
