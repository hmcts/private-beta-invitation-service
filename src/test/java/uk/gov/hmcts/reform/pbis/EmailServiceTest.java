package uk.gov.hmcts.reform.pbis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pbis.model.EmailToSend;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.notify.NotificationClientProvider;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;


@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    private EmailService emailService;

    @Mock
    private NotificationClientApi notificationClient;

    @Mock
    private NotificationClientProvider notificationClientProvider;

    @Mock
    private EmailCreator emailCreator;

    private final PrivateBetaRegistration privateBetaRegistration = new PrivateBetaRegistration(
        "reference id 123",
        "service 123",
        "email@example.com",
        "first name",
        "last name"
    );

    private final EmailToSend emailToSend = new EmailToSend(
        "email@example.com",
        "template id 123",
        ImmutableMap.of("name", "value"),
        "reference id 123"
    );


    @Before
    public void setUp() {
        when(notificationClientProvider.getClient(anyString())).thenReturn(notificationClient);
        emailService = new EmailService(notificationClientProvider, emailCreator);
    }

    @Test
    public void sendWelcomeEmail_should_call_email_client_with_email_data()
        throws NotificationClientException {

        given(emailCreator.createEmailToSend(privateBetaRegistration)).willReturn(emailToSend);

        emailService.sendWelcomeEmail(privateBetaRegistration);

        verify(emailCreator).createEmailToSend(privateBetaRegistration);
        verify(notificationClientProvider).getClient(privateBetaRegistration.service);

        verify(notificationClient).sendEmail(
            emailToSend.templateId,
            emailToSend.emailAddress,
            emailToSend.templateFields,
            emailToSend.referenceId
        );

        verifyNoMoreInteractions(emailCreator, notificationClient, notificationClientProvider);
    }

    @Test()
    public void sendWelcomeEmail_should_throw_exception_when_email_creator_fails() {
        given(
            emailCreator.createEmailToSend(any())
        ).willThrow(
            new EmailSendingException("test exception", null)
        );


        assertThatThrownBy(
            () -> emailService.sendWelcomeEmail(privateBetaRegistration)
        ).isInstanceOf(
            EmailSendingException.class
        );
    }

    @Test()
    public void sendWelcomeEmail_should_throw_exception_when_notification_client_fails()
        throws NotificationClientException {

        given(
            notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString())
        ).willThrow(
            mock(NotificationClientException.class)
        );

        assertThatThrownBy(
            () -> emailService.sendWelcomeEmail(privateBetaRegistration)
        ).isInstanceOf(
            EmailSendingException.class
        );
    }

    @Test()
    public void sendWelcomeEmail_should_throw_exception_when_client_provider_fails()
        throws NotificationClientException {

        Exception thrownException = new RuntimeException("test");

        given(notificationClientProvider.getClient(anyString())).willThrow(thrownException);

        assertThatThrownBy(
            () -> emailService.sendWelcomeEmail(privateBetaRegistration)
        ).isInstanceOf(
            EmailSendingException.class
        ).hasMessage(
            "Failed to send email. Reference ID: " + privateBetaRegistration.referenceId
        ).hasCause(thrownException);
    }

    @Test()
    public void sendWelcomeEmail_should_rethrow_exception_when_service_not_found()
        throws NotificationClientException {

        ServiceNotFoundException exceptionThrown = new ServiceNotFoundException("test");

        given(notificationClientProvider.getClient(anyString())).willThrow(exceptionThrown);

        assertThatThrownBy(
            () -> emailService.sendWelcomeEmail(privateBetaRegistration)
        ).isSameAs(exceptionThrown);
    }
}
