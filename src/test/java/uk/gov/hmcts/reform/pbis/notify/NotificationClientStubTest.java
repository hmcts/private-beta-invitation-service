package uk.gov.hmcts.reform.pbis.notify;

import org.junit.Test;
import uk.gov.service.notify.SendEmailResponse;

import static java.util.Collections.EMPTY_MAP;
import static org.assertj.core.api.Assertions.assertThat;

public class NotificationClientStubTest {

    private final NotificationClientStub notificationClientStub = new NotificationClientStub();

    @Test
    public void sendEmail_should_return_null() throws Exception {
        SendEmailResponse response = notificationClientStub.sendEmail(
            "template id 123",
            "email@example.com",
            EMPTY_MAP,
            "reference 123"
        );

        assertThat(response).isNull();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendSms_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.sendSms("", "", EMPTY_MAP, "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendLetter_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.sendLetter("", EMPTY_MAP, "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getNotificationById_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.getNotificationById("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getNotifications_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.getNotifications("", "", "", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getTemplateById_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.getTemplateById("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getTemplateVersion_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.getTemplateVersion("", 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllTemplates_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.getAllTemplates("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getReceivedTextMessages_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.getReceivedTextMessages("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendSms_senderId_should_throw_UnsupportedOperationException() throws Exception {
        notificationClientStub.sendSms(null, null, null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void generateTemplatePreview_should_throw_UnsupportedOperationException()
        throws Exception {

        notificationClientStub.generateTemplatePreview("", EMPTY_MAP);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendPrecompiledLetter_with_postage_should_throw_UnsupportedOperationException() {
        notificationClientStub.sendPrecompiledLetter(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendPrecompiledLetter_should_throw_UnsupportedOperationException() {
        notificationClientStub.sendPrecompiledLetter(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendPrecompiledLetterWithInputStream_should_throw_UnsupportedOperationException() {
        notificationClientStub.sendPrecompiledLetterWithInputStream(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sendPrecompiledLetterWithInputStream_with_postage_should_throw_UnsupportedOperationException() {
        notificationClientStub.sendPrecompiledLetterWithInputStream(null, null, null);
    }
}
