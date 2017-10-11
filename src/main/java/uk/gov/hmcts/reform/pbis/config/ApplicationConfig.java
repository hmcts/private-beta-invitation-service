package uk.gov.hmcts.reform.pbis.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;

@Configuration
@ConfigurationProperties
public class ApplicationConfig {

    @Value("${notify.useStub}")
    private boolean useNotifyClientStub;

    private final List<EmailTemplateMapping> emailTemplateMappings = new ArrayList<>();

    public List<EmailTemplateMapping> getEmailTemplateMappings() {
        return emailTemplateMappings;
    }

    public boolean getUseNotifyClientStub() {
        return useNotifyClientStub;
    }

}
