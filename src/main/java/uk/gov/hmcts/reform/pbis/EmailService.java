package uk.gov.hmcts.reform.pbis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pbis.model.EmailToSend;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.notify.NotificationClientProvider;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

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

    public void sendWelcomeEmail(PrivateBetaRegistration privateBetaRegistration) {
        logger.info(String.format(
            "Sending welcome email. Reference ID: %s", privateBetaRegistration.referenceId
        ));

        try {
            EmailToSend emailToSend = emailCreator.createEmailToSend(privateBetaRegistration);

            NotificationClientApi notificationClient =
                notificationClientProvider.getClient(privateBetaRegistration.service);

            sendEmail(emailToSend, notificationClient);

            logger.info(String.format(
                "Welcome email sent. Reference ID: %s", privateBetaRegistration.referenceId
            ));
        } catch (ServiceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format(
                "Failed to send email. Reference ID: %s",
                privateBetaRegistration.referenceId
            );

            throw new EmailSendingException(errorMessage, e);
        }
    }

    private void sendEmail(
        EmailToSend emailToSend,
        NotificationClientApi notificationClient
    ) throws NotificationClientException {

        notificationClient.sendEmail(
            emailToSend.templateId,
            emailToSend.emailAddress,
            emailToSend.templateFields,
            emailToSend.referenceId
        );
    }
}
