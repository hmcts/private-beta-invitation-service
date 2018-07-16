package uk.gov.hmcts.reform.pbis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pbis.notify.EmailToSend;
import uk.gov.hmcts.reform.pbis.servicebus.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.notify.NotificationClientProvider;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final NotificationClientProvider notificationClientProvider;
    private final EmailCreator emailCreator;

    @Autowired
    public EmailService(
        NotificationClientProvider notificationClientProvider,
        EmailCreator emailCreator
    ) {
        this.notificationClientProvider = notificationClientProvider;
        this.emailCreator = emailCreator;
    }

    public void sendWelcomeEmail(PrivateBetaRegistration reg) {
        logger.info("Sending welcome email. Reference ID: {}", reg.referenceId);

        try {
            EmailToSend emailToSend = emailCreator.createEmailToSend(reg);

            notificationClientProvider
                .getClient(reg.service)
                .sendEmail(
                    emailToSend.templateId,
                    emailToSend.emailAddress,
                    emailToSend.templateFields,
                    emailToSend.referenceId
                );

            logger.info("Welcome email sent. Reference ID: {}", reg.referenceId);
        } catch (ServiceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format(
                "Failed to send email. Reference ID: %s",
                reg.referenceId
            );

            throw new EmailSendingException(errorMessage, e);
        }
    }

}
