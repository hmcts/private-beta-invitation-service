package uk.gov.hmcts.reform.pbis.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pbis.EmailCreator;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.hmcts.reform.pbis.notify.NotificationClientProvider;

@Configuration
@ConfigurationProperties
public class ApplicationConfig {

    @Value("${notify.useStub}")
    private boolean useNotifyClientStub;

    private final List<EmailTemplateMapping> emailTemplateMappings = new ArrayList<>();

    // this getter is needed by the framework
    public List<EmailTemplateMapping> getEmailTemplateMappings() {
        return emailTemplateMappings;
    }

    @Bean
    public EmailCreator getEmailCreator() {
        return new EmailCreator(emailTemplateMappings);
    }

    @Bean
    public NotificationClientProvider getNotificationClientProvider() {
        return new NotificationClientProvider(emailTemplateMappings, useNotifyClientStub);
    }
}
