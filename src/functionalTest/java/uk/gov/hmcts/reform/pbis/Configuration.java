package uk.gov.hmcts.reform.pbis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


public class Configuration {

    private final Config config = ConfigFactory.load("environment.conf");

    public String getNotifyApiKey() {
        return config.getString("notify.notifyApiKey");
    }

    public String getTemplateId() {
        return config.getString("notify.templateId");
    }

    public String getServiceName() {
        return config.getString("notify.service");
    }

    public String getWelcomeLink() {
        return config.getString("notify.welcomeLink");
    }

    public String getServiceBusConnectionString() {
        return config.getString("serviceBus.connectionString");
    }

    public long getServiceBusPollingDelayInMs() {
        return config.getLong("serviceBus.pollingDelayInMs");
    }
}
