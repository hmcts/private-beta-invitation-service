package uk.gov.hmcts.reform.pbis.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pbis.EmailService;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

/**
 * This service processes Azure Service Bus subscription queue.
 *
 * <p>It reads the whole queue and sends a welcome email based on the content of each message.</p>
 */
@Service
public class MessageQueueProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueProcessor.class);

    private final IServiceBusClientFactory clientFactory;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MessageQueueProcessor(
        IServiceBusClientFactory clientFactory,
        EmailService emailService
    ) {
        this.clientFactory = clientFactory;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelayString = "${serviceBus.pollingDelayInMs}")
    public void run() {
        logger.info("Processing messages from subscription queue.");

        try (IServiceBusClient serviceBusClient = clientFactory.createClient()) {
            processMessages(serviceBusClient);
        } catch (Exception e) {
            logger.error("An error occurred when processing messages from subscription queue.", e);
        }
    }

    private void processMessages(IServiceBusClient serviceBusClient) {
        int messageCount = 0;
        int failureCount = 0;
        IMessage message;

        while ((message = serviceBusClient.receiveMessage()) != null) {
            logger.info(String.format("Received message with ID %s.", message.getMessageId()));
            messageCount++;

            boolean messageProcessedSuccessfully = processMessage(message);

            if (messageProcessedSuccessfully) {
                serviceBusClient.completeMessage(message.getMessageId(), message.getLockToken());
                logger.info(String.format(
                    "Completed processing message with ID %s on attempt %s.",
                    message.getMessageId(),
                    message.getDeliveryCount())
                );
            } else {
                failureCount++;
            }
        }

        logger.info(String.format(
            "No more messages to process. Total: %s, failed: %s.", messageCount, failureCount)
        );
    }

    private boolean processMessage(IMessage message) {
        try {
            PrivateBetaRegistration registration =
                objectMapper.readValue(message.getBody(), PrivateBetaRegistration.class);

            emailService.sendWelcomeEmail(registration);

            return true;
        } catch (Exception e) {
            logger.error(
                String.format(
                    "Failed to process message with ID %s on attempt %s.",
                    message.getMessageId(),
                    message.getDeliveryCount()
                ),
                e
            );

            return false;
        }
    }

}
