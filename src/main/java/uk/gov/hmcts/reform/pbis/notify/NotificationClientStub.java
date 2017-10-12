package uk.gov.hmcts.reform.pbis.notify;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;
import uk.gov.service.notify.Template;
import uk.gov.service.notify.TemplateList;
import uk.gov.service.notify.TemplatePreview;


public class NotificationClientStub implements NotificationClientApi {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClientStub.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE =
        "Notification client stub doesn't support this method";

    @Override
    public SendEmailResponse sendEmail(
        String templateId,
        String emailAddress,
        Map<String, String> personalisation,
        String reference
    ) throws NotificationClientException {
        logger.info(
            String.format(
                "Sending email to %s. Template ID: %s, reference: %s",
                emailAddress,
                templateId,
                reference
            )
        );

        return null;
    }

    @Override
    public SendSmsResponse sendSms(
        String templateId,
        String phoneNumber,
        Map<String, String> personalisation,
        String reference
    ) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public SendLetterResponse sendLetter(
        String templateId,
        Map<String, String> personalisation,
        String reference
    ) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public Notification getNotificationById(
        String notificationId
    ) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public NotificationList getNotifications(
        String status,
        String notificationType,
        String reference,
        String olderThanId
    ) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public Template getTemplateById(String templateId) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public Template getTemplateVersion(
        String templateId, int version
    ) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public TemplateList getAllTemplates(String templateType) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public TemplatePreview generateTemplatePreview(
        String templateId,
        Map<String, String> personalisation
    ) throws NotificationClientException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }
}
