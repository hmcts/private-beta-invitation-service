package uk.gov.hmcts.reform.pbis.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pbis.EmailService;
import uk.gov.hmcts.reform.pbis.MessageProcessingResult;
import uk.gov.hmcts.reform.pbis.ServiceNotFoundException;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.invalidMessageData;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.invalidMessageFormat;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.processingError;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.success;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResult.unknownService;

/**
 * This service processes Azure Service Bus subscription queue.
 *
 * <p>It reads the whole queue and sends a welcome email based on the content of each message.</p>
 */
@Service
@ConditionalOnProperty(value = "scheduling.enable", havingValue = "true", matchIfMissing = true)
public class MessageQueueProcessor {

    private final IServiceBusClientFactory clientFactory;
    private final EmailService emailService;
    private final Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageQueueProcessingTracker tracker;


    @Autowired
    public MessageQueueProcessor(
        IServiceBusClientFactory clientFactory,
        EmailService emailService,
        Validator validator,
        MessageQueueProcessingTracker tracker
    ) {
        this.clientFactory = clientFactory;
        this.emailService = emailService;
        this.validator = validator;
        this.tracker = tracker;
    }

    @Scheduled(fixedDelayString = "${serviceBus.pollingDelayInMs}")
    public void run() {
        tracker.trackProcessingStarted();

        try (IServiceBusClient serviceBusClient = clientFactory.createClient()) {
            processMessages(serviceBusClient);
            tracker.trackProcessingCompleted();
        } catch (Exception e) {
            tracker.trackProcessingError(e);
        }
    }

    private void processMessages(IServiceBusClient serviceBusClient) {
        IMessage message;

        while ((message = serviceBusClient.receiveMessage()) != null) {
            tracker.trackReceivedMessage(message.getMessageId());

            MessageProcessingResult processingResult = processMessage(message);
            updateMessageInSubscription(message, processingResult, serviceBusClient);
            tracker.trackMessageProcessingResult(processingResult, message);
        }
    }

    private void updateMessageInSubscription(
        IMessage message,
        MessageProcessingResult processingResult,
        IServiceBusClient serviceBusClient
    ) {
        switch (processingResult.resultType) {
            case SUCCESS:
                completeMessage(message, serviceBusClient);
                break;
            case UNPROCESSABLE_MESSAGE:
                sendToDeadLetter(message, serviceBusClient, processingResult.errorDetails);
                break;
            default:
                // let the message lock expire before it's available again
                break;
        }
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

    private void completeMessage(IMessage message, IServiceBusClient serviceBusClient) {
        serviceBusClient.completeMessage(message.getMessageId(), message.getLockToken());
    }

    private void sendToDeadLetter(
        IMessage message,
        IServiceBusClient serviceBusClient,
        MessageProcessingResult.ProcessingError error
    ) {
        serviceBusClient.sendToDeadLetter(
            message,
            error.reason,
            error.description,
            error.fieldValidationErrors
        );
    }

    private MessageProcessingResult sendEmail(PrivateBetaRegistration registration) {
        try {
            emailService.sendWelcomeEmail(registration);
            return success();
        } catch (ServiceNotFoundException e) {
            return unknownService();
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


}
