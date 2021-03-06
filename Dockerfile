ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.4

# Mandatory!
ENV APP private-beta-invitation-service-all.jar

COPY build/libs/$APP /opt/app/
COPY lib/applicationinsights-agent-2.5.1.jar lib/AI-Agent.xml /opt/app/

EXPOSE 4700

CMD ["private-beta-invitation-service-all.jar"]
