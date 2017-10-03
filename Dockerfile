FROM openjdk:8-jre

COPY build/install/private-beta-invitation-service /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:4700/health

ENTRYPOINT ["/opt/app/bin/private-beta-invitation-service"]
