# Dockerfile to use for running tests. Includes JDK, Docker and Docker-Compose.
ARG OPENJDK_VERSION=17.0.2
FROM openjdk:$OPENJDK_VERSION-jdk-slim

RUN apt-get update && \
    apt-get install -y curl

RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh
