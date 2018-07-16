package uk.gov.hmcts.reform.pbis.config;

public class EmailTemplateMapping {

    private String service;
    private String templateId;
    private String notifyApiKey;
    private String welcomeLink;


    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getNotifyApiKey() {
        return notifyApiKey;
    }

    public void setNotifyApiKey(String notifyApiKey) {
        this.notifyApiKey = notifyApiKey;
    }

    public String getWelcomeLink() {
        return welcomeLink;
    }

    public void setWelcomeLink(String welcomeLink) {
        this.welcomeLink = welcomeLink;
    }
}
