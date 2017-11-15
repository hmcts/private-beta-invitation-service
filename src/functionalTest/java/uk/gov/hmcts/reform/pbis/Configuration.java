package uk.gov.hmcts.reform.pbis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.time.Duration;


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

    public String getServiceBusNamespaceConnectionString() {
        return config.getString("serviceBus.namespaceConnectionString");
    }

    public String getServiceBusTopicName() {
        return config.getString("serviceBus.topic");
    }

    public String getServiceBusSubscriptionName() {
        return config.getString("serviceBus.subscription");
    }

    public long getServiceBusPollingDelayInMs() {
        return config.getLong("serviceBus.pollingDelayInMs");
    }

    public long getMessageLockTimeoutInMs() {
        return config.getLong("serviceBus.messageLockTimeoutInMs");
    }

    public Duration getMaxReceiveWaitTime() {
        return Duration.ofMillis(
            config.getLong("serviceBus.maxReceiveWaitTimeInMs")
        );
    }
}
