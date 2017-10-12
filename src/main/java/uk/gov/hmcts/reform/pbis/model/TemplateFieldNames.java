package uk.gov.hmcts.reform.pbis.model;

public enum TemplateFieldNames {

    FIRST_NAME("first name"),
    LAST_NAME("last name"),
    WELCOME_LINK("welcome link");

    private final String fieldName;

    TemplateFieldNames(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
