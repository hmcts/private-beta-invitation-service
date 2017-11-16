package uk.gov.hmcts.reform.pbis.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.pbis.Configuration;
import uk.gov.hmcts.reform.pbis.EmailCreator;
import uk.gov.hmcts.reform.pbis.EmailService;
import uk.gov.hmcts.reform.pbis.ServiceNotFoundException;
import uk.gov.hmcts.reform.pbis.categories.IntegrationTests;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;
import uk.gov.hmcts.reform.pbis.notify.NotificationClientProvider;
import uk.gov.hmcts.reform.pbis.utils.NotificationHelper;
import uk.gov.hmcts.reform.pbis.utils.SampleData;
import uk.gov.service.notify.Notification;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties =
    {
        // prevent scheduled jobs from running
        "scheduling.enable=false"
    }
)
@Category(IntegrationTests.class)
public class EmailServiceTest {

    private static final Configuration testConfig = new Configuration();

    @Autowired
    private NotificationClientProvider notificationClientProvider;

    @Autowired
    private EmailCreator emailCreator;

    private EmailService emailService;
    private NotificationHelper notificationHelper;

    @Before
    public void setUp() {
        emailService = new EmailService(notificationClientProvider, emailCreator);
        notificationHelper = new NotificationHelper(testConfig.getNotifyApiKey());
    }

    @Test
    public void sendWelcomeEmail_should_send_email_when_service_is_recognised() {
        PrivateBetaRegistration registration =
            SampleData.getSampleRegistration(testConfig.getServiceName());

        emailService.sendWelcomeEmail(registration);

        List<Notification> sentEmails = notificationHelper.getSentEmails(registration.referenceId);

        assertThat(sentEmails).hasSize(1);

        Notification sentEmail = sentEmails.get(0);

        assertEmailContainsRightData(sentEmail, registration);
    }

    @Test
    public void sendWelcomeEmail_should_fail_when_service_is_unknown() {
        String service = "unknown-service";

        PrivateBetaRegistration registrationWithUnknownService =
            SampleData.getSampleRegistration(service);

        assertThatThrownBy(() ->
            emailService.sendWelcomeEmail(registrationWithUnknownService)
        )
            .isInstanceOf(ServiceNotFoundException.class)
            .hasMessageContaining(
                String.format(
                    "Service %s not found in email template configuration",
                    service
                )
            );

        List<Notification> sentEmails =
            notificationHelper.getSentEmails(registrationWithUnknownService.referenceId);

        assertThat(sentEmails).as("check if no email was sent").isEmpty();
    }

    private void assertEmailContainsRightData(
        Notification email,
        PrivateBetaRegistration registration
    ) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(email.getTemplateId().toString())
            .as("check template ID")
            .isEqualTo(testConfig.getTemplateId());

        softly.assertThat(email.getNotificationType())
            .as("check notification type")
            .isEqualTo("email");

        softly.assertThat(email.getEmailAddress()).as("check email address")
            .isEqualTo(Optional.of(registration.emailAddress));

        softly.assertThat(email.getReference())
            .as("check email reference")
            .isEqualTo(Optional.of(registration.referenceId));

        softly.assertThat(email.getBody())
            .as("check email body")
            .contains("First name: " + registration.firstName)
            .contains("Last name: " + registration.lastName)
            .contains("Welcome link: " + testConfig.getWelcomeLink());

        softly.assertAll();
    }
}
