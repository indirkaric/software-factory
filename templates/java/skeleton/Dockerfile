# syntax=docker/dockerfile:1
# The image is always built from the checkout it is built in: Maven runs inside the build stage,
# never a jar that happens to sit in target/. Build it with `dev-rebuild {{service-name}}` in the
# development repo (it passes GIT_SHA, so the image says which commit it runs).

# 1. build the jar from the sources
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /build
# dependencies first: a rebuild without a pom.xml change reuses this layer and the ~/.m2 cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -ntp -DskipTests package

# 2. a small JRE
FROM amazoncorretto:17.0.9-alpine AS corretto-jdk
# FROM public.ecr.aws/docker/library/amazoncorretto:17.0.9-alpine AS corretto-jdk

# NOTE: no --strip-debug here. jlink's --strip-debug shells out to objcopy via
# the JDK's jspawnhelper, which reliably fails under QEMU user-mode emulation
# (buildx arm64 cross-build on an amd64 runner) with:
#   "Cannot run program objcopy ... Failed to exec spawn helper"
# Dropping --strip-debug avoids the subprocess spawn entirely. Costs a
# slightly larger JRE (debug symbols retained); add it back behind a
# TARGETARCH check if you want it stripped on native amd64 builds only.
RUN $JAVA_HOME/bin/jlink \
    --verbose \
    --add-modules ALL-MODULE-PATH \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /customjre

# 3. main app image
# FROM public.ecr.aws/docker/library/alpine:latest
FROM alpine:latest
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.revision=$GIT_SHA
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apk update

# copy JRE from the base image
COPY --from=corretto-jdk /customjre $JAVA_HOME

# Add app user
ARG APPLICATION_USER=appuser
RUN adduser --no-create-home -u 1000 -D $APPLICATION_USER

# Configure working directory
RUN mkdir /app && \
    chown -R $APPLICATION_USER /app

USER 1000

COPY --from=build --chown=1000:1000 /build/target/{{service-name}}.jar /app/{{service-name}}.jar
WORKDIR /app

EXPOSE {{port}}
ENTRYPOINT [ "/jre/bin/java", "-jar", "/app/{{service-name}}.jar" ]
