package uk.gov.hmcts.reform.pbis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.hmcts.reform.pbis.model.EmailToSend;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


@RunWith(MockitoJUnitRunner.class)
public class EmailCreatorTest {

    private static final String FIRST_NAME_FIELD = "first name";
    private static final String LAST_NAME_FIELD = "last name";
    private static final String WELCOME_LINK_FIELD = "welcome link";

    private static final EmailTemplateMapping mapping1 =
        createMapping("service1", "templateId1", "apiKey1", "welcomeLink1");

    private static final EmailTemplateMapping mapping2 =
        createMapping("service2", "templateId2", "apiKey2", "welcomeLink2");

    private static final List<EmailTemplateMapping> emailTemplateMappings =
        Lists.newArrayList(mapping1, mapping2);

    private EmailCreator emailCreator;


    @Before
    public void setUp() {
        emailCreator = new EmailCreator(emailTemplateMappings);
    }

    @Test(expected = ServiceNotFoundException.class)
    public void createEmailToSend_should_throw_exception_when_service_not_found() {
        emailCreator.createEmailToSend(getPrivateBetaRegistration("unrecognised service"));
    }

    @Test
    public void createEmailToSend_should_map_service_to_the_right_template() {
        PrivateBetaRegistration registration = getPrivateBetaRegistration(mapping1.getService());
        EmailToSend email = emailCreator.createEmailToSend(registration);

        assertThat(email.templateId).isEqualTo(mapping1.getTemplateId());
    }

    @Test
    public void createEmailToSend_should_return_email_with_data_from_registration() {
        PrivateBetaRegistration registration = getPrivateBetaRegistration(mapping2.getService());
        EmailToSend email = emailCreator.createEmailToSend(registration);

        assertThat(email.emailAddress).isEqualTo(registration.emailAddress);
        assertThat(email.referenceId).isEqualTo(registration.referenceId);

        assertThat(email.templateFields.keySet()).containsExactlyInAnyOrder(
            FIRST_NAME_FIELD, LAST_NAME_FIELD, WELCOME_LINK_FIELD
        );

        assertThat(email.templateFields.get(FIRST_NAME_FIELD)).isEqualTo(registration.firstName);
        assertThat(email.templateFields.get(LAST_NAME_FIELD)).isEqualTo(registration.lastName);
        assertThat(
            email.templateFields.get(WELCOME_LINK_FIELD)).isEqualTo(mapping2.getWelcomeLink()
        );
    }

    private PrivateBetaRegistration getPrivateBetaRegistration(String service) {
        return new PrivateBetaRegistration(
            "reference id",
            service,
            "email@example.com",
            "firstname",
            "lastname"
        );
    }

    private static EmailTemplateMapping createMapping(
        String service, String templateId, String apiKey, String welcomeLink
    ) {
        EmailTemplateMapping mapping = new EmailTemplateMapping();
        mapping.setService(service);
        mapping.setTemplateId(templateId);
        mapping.setNotifyApiKey(apiKey);
        mapping.setWelcomeLink(welcomeLink);

        return mapping;
    }
}
