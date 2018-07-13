package uk.gov.hmcts.reform.pbis;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.hmcts.reform.pbis.model.EmailToSend;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.FIRST_NAME;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.LAST_NAME;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.WELCOME_LINK;

@RunWith(MockitoJUnitRunner.class)
public class EmailCreatorTest {

    private static final EmailTemplateMapping mapping1 =
        createMapping("service1", "templateId1", "apiKey1", "welcomeLink1");

    private static final EmailTemplateMapping mapping2 =
        createMapping("service2", "templateId2", "apiKey2", "welcomeLink2");

    private static final List<EmailTemplateMapping> emailTemplateMappings = Lists.newArrayList(mapping1, mapping2);

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
        // given
        PrivateBetaRegistration registration = getPrivateBetaRegistration(mapping1.getService());

        // when
        EmailToSend email = emailCreator.createEmailToSend(registration);

        // then
        assertThat(email.templateId).isEqualTo(mapping1.getTemplateId());
    }

    @Test
    public void createEmailToSend_should_return_email_with_data_from_registration() {
        // given
        PrivateBetaRegistration registration = getPrivateBetaRegistration(mapping2.getService());

        // when
        EmailToSend email = emailCreator.createEmailToSend(registration);

        // then
        assertThat(email.emailAddress).isEqualTo(registration.emailAddress);
        assertThat(email.referenceId).isEqualTo(registration.referenceId);

        assertThat(email.templateFields.keySet()).containsExactlyInAnyOrder(
            FIRST_NAME, LAST_NAME, WELCOME_LINK
        );

        assertThat(email.templateFields.get(FIRST_NAME)).isEqualTo(registration.firstName);
        assertThat(email.templateFields.get(LAST_NAME)).isEqualTo(registration.lastName);
        assertThat(email.templateFields.get(WELCOME_LINK)).isEqualTo(mapping2.getWelcomeLink());
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
