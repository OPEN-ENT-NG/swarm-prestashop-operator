####
# Multi-stage Dockerfile for Swarm Prestashop Operator (Quarkus Operator SDK)
#
# This Dockerfile builds the Prestashop Kubernetes Operator
#
# Build arguments:
#   NEXUS_USERNAME: Username for Nexus repository authentication
#   NEXUS_PASSWD: Password for Nexus repository authentication
#
# Build the image:
#   docker build --build-arg NEXUS_PASSWD=your_password -t swarm-prestashop-operator:latest .
####

###
# Stage 1: Build stage using Maven
###
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Build arguments for Maven repository authentication
ARG NEXUS_USERNAME=jenkins
ARG NEXUS_PASSWD=ZaGRnB3070e4

# Install required packages
RUN apk add --no-cache git

# Set working directory
WORKDIR /build

# Create .m2 directory and copy Maven settings
RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

# Copy Maven wrapper and pom.xml first
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

# Fix line endings and make mvnw executable
RUN chmod +x mvnw && \
    dos2unix mvnw || sed -i 's/\r$//' mvnw || true

# Verify settings.xml is correct
RUN cat /root/.m2/settings.xml && echo "Settings file OK"

# Download dependencies
RUN mvn dependency:go-offline -B -s /root/.m2/settings.xml

# Copy source code
COPY src/ src/

# Build the application
RUN mvn package -DskipTests -B -s /root/.m2/settings.xml && \
    ls -lh target/quarkus-app/

###
# Stage 2: Runtime stage
###
FROM registry.access.redhat.com/ubi8/openjdk-21:1.19

# Metadata labels
LABEL maintainer="Edifice" \
      description="Swarm Prestashop Operator - K8s Operator for Prestashop deployments" \
      version="0.1.0-dev" \
      java.version="21"

ENV LANGUAGE='en_US:en'

WORKDIR /deployments

# Copy the built artifacts
COPY --from=builder --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080

USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/q/health/ready || exit 1

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
