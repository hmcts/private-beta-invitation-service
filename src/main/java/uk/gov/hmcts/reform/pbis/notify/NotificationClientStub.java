package uk.gov.hmcts.reform.pbis.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationList;
import uk.gov.service.notify.ReceivedTextMessageList;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;
import uk.gov.service.notify.Template;
import uk.gov.service.notify.TemplateList;
import uk.gov.service.notify.TemplatePreview;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public class NotificationClientStub implements NotificationClientApi {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClientStub.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE =
        "Notification client stub doesn't support this method";

    @Override
    public SendEmailResponse sendEmail(
        String templateId, String emailAddress, Map<String, ?> personalisation, String reference
    ) {
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
    public SendEmailResponse sendEmail(
        String templateId, String emailAddress, Map<String, ?> personalisation, String reference, String emailReplyToId
    ) {
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
        String templateId, String phoneNumber, Map<String, ?> personalisation, String reference
    ) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public SendSmsResponse sendSms(
        String templateId, String phoneNumber, Map<String, ?> personalisation, String reference, String smsSenderId
    ) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public SendLetterResponse sendLetter(String templateId, Map<String, ?> personalisation, String reference) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public LetterResponse sendPrecompiledLetter(String reference, File precompiledPDF) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public LetterResponse sendPrecompiledLetter(String reference, File precompiledPDF, String postage) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public LetterResponse sendPrecompiledLetterWithInputStream(String reference, InputStream stream) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public LetterResponse sendPrecompiledLetterWithInputStream(String reference, InputStream stream, String postage) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public Notification getNotificationById(String notificationId) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public NotificationList getNotifications(
        String status,
        String notificationType,
        String reference,
        String olderThanId
    ) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public Template getTemplateById(String templateId) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public Template getTemplateVersion(String templateId, int version) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public TemplateList getAllTemplates(String templateType) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public TemplatePreview generateTemplatePreview(String templateId, Map<String, Object> personalisation) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public ReceivedTextMessageList getReceivedTextMessages(String olderThanId) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }
}
