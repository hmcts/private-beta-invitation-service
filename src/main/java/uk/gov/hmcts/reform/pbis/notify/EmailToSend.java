package uk.gov.hmcts.reform.pbis.notify;

import java.util.Map;

public final class EmailToSend {

    public final String emailAddress;
    public final String templateId;
    public final Map<String, String> templateFields;
    public final String referenceId;


    public EmailToSend(
        final String emailAddress,
        final String templateId,
        final Map<String, String> templateFields,
        final String referenceId
    ) {

        this.emailAddress = emailAddress;
        this.templateId = templateId;
        this.templateFields = templateFields;
        this.referenceId = referenceId;
    }
}
