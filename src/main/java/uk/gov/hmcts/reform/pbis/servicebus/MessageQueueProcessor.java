package uk.gov.hmcts.reform.pbis.servicebus;

import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.invalidMessageData;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.invalidMessageFormat;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.processingError;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.success;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResultType.SUCCESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pbis.EmailService;
import uk.gov.hmcts.reform.pbis.MessageProcessingResult;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


/**
 * This service processes Azure Service Bus subscription queue.
 *
 * <p>It reads the whole queue and sends a welcome email based on the content of each message.</p>
 */
@Service
@ConditionalOnProperty(value = "scheduling.enable", havingValue = "true", matchIfMissing = true)
public class MessageQueueProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueProcessor.class);

    private final IServiceBusClientFactory clientFactory;
    private final EmailService emailService;
    private final Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MessageQueueProcessor(
        IServiceBusClientFactory clientFactory,
        EmailService emailService,
        Validator validator

    ) {
        this.clientFactory = clientFactory;
        this.emailService = emailService;
        this.validator = validator;
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

            MessageProcessingResult processingResult = processMessage(message);

            if (processingResult.resultType == SUCCESS) {
                completeMessage(message, serviceBusClient);
            } else {
                failureCount++;
            }

            logProcessingResult(processingResult, message);
        }

        logger.info(String.format(
            "No more messages to process. Total: %s, failed: %s.", messageCount, failureCount)
        );
    }

    private MessageProcessingResult processMessage(IMessage message) {
        try {
            Optional<PrivateBetaRegistration> registrationOption = tryReadRegistration(message);

            return registrationOption.map(registration -> {
                    Set<ConstraintViolation<PrivateBetaRegistration>> violations =
                        validator.validate(registration);

                    if (violations.isEmpty()) {
                        return sendEmail(registration);
                    } else {
                        return invalidMessageData(violations);
                    }
                }
            ).orElse(invalidMessageFormat());
        } catch (Exception e) {
            return processingError(e);
        }
    }

    private void logProcessingResult(MessageProcessingResult processingResult, IMessage message) {
        switch (processingResult.resultType) {
            case SUCCESS:
                logMessageProcessingSuccess(message);
                break;
            case ERROR:
                logMessageProcessingFailure(processingResult.errorDetails, message);
                break;
            case UNPROCESSABLE_MESSAGE:
                logUnprocessableMessage(processingResult.errorDetails, message);
                break;
            default:
        }
    }

    private void completeMessage(IMessage message, IServiceBusClient serviceBusClient) {
        serviceBusClient.completeMessage(message.getMessageId(), message.getLockToken());
    }

    private MessageProcessingResult sendEmail(PrivateBetaRegistration registration) {
        try {
            emailService.sendWelcomeEmail(registration);
            return success();
        } catch (Exception e) {
            return processingError(e);
        }
    }

    private Optional<PrivateBetaRegistration> tryReadRegistration(IMessage message) {
        try {
            return Optional.of(
                objectMapper.readValue(message.getBody(), PrivateBetaRegistration.class)
            );
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void logMessageProcessingSuccess(IMessage message) {
        logger.info(String.format(
            "Completed processing message with ID %s on attempt %s.",
            message.getMessageId(),
            message.getDeliveryCount() + 1)
        );
    }

    private void logUnprocessableMessage(
        MessageProcessingResult.ProcessingError processingError,
        IMessage message
    ) {
        String invalidFieldDetails = processingError.invalidFields != null
            ? " (" + processingError.invalidFields + ")"
            : "";

        logger.warn(
            String.format(
                "Rejected message with ID %s. Reason: %s - %s%s",
                message.getMessageId(),
                processingError.reason,
                processingError.description,
                invalidFieldDetails
            )
        );
    }

    private void logMessageProcessingFailure(
        MessageProcessingResult.ProcessingError processingError,
        IMessage message
    ) {
        logger.error(
            String.format(
                "Failed to process message with ID %s on attempt %s.",
                message.getMessageId(),
                message.getDeliveryCount() + 1
            ),
            processingError.exception
        );
    }
}
