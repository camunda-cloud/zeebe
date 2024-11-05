#!/bin/bash

# set constants
CAMUNDA_VERSION="8.7.0-alpha1"
CAMUNDA_CONNECTORS_VERSION="8.7.0-alpha1"
ELASTICSEARCH_VERSION="8.13.4"

architectureRaw="$(uname -m)"
case "${architectureRaw}" in
  arm64*)     architecture=aarch64;;
  x86_64*)    architecture=x86_64;;
  *)          architecture=UNKNOWN
esac

unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    *)          machine="UNKNOWN:${unameOut}"
esac

if [ "$machine" == "Mac" ]; then
    export PLATFORM=darwin
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    export PLATFORM=linux
fi

if [ -z "$JAVA_ARTIFACTS_USER" ] || [ -z "$JAVA_ARTIFACTS_PASSWORD" ]; then
  echo "Error: JAVA_ARTIFACTS_USER or JAVA_ARTIFACTS_PASSWORD env vars are not set or are empty."
  exit 1
fi

# Retrieve elasticsearch
if [ ! -d "elasticsearch-$ELASTICSEARCH_VERSION" ]; then
  wget -q "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ELASTICSEARCH_VERSION}-${PLATFORM}-${architecture}.tar.gz"
  tar -xzf elasticsearch-${ELASTICSEARCH_VERSION}-${PLATFORM}-${architecture}.tar.gz
fi

if [ ! -d "camunda-zeebe-$CAMUNDA_VERSION" ]; then
  gh release download -R camunda/camunda "$CAMUNDA_VERSION" -p "camunda-zeebe-$CAMUNDA_VERSION.tar.gz"
  tar -xzf camunda-zeebe-$CAMUNDA_VERSION.tar.gz
fi

connectorsFileName="connector-runtime-bundle-$CAMUNDA_CONNECTORS_VERSION-with-dependencies.jar"
if [ ! -f "$connectorsFileName" ]; then
  wget -q --user="$JAVA_ARTIFACTS_USER" --password="$JAVA_ARTIFACTS_PASSWORD" "https://repository.nexus.camunda.cloud/content/groups/internal/io/camunda/connector/connector-runtime-bundle/$CAMUNDA_CONNECTORS_VERSION/$connectorsFileName"
fi

tar -czf camunda8-run-$CAMUNDA_VERSION-$PLATFORM-$architecture.tar.gz \
  -C ../ \
  c8run/start.sh \
  c8run/shutdown.sh \
  c8run/endpoints.txt \
  c8run/README.md \
  c8run/connectors-application.properties \
  c8run/"$connectorsFileName" \
  c8run/internal/run.sh \
  c8run/"elasticsearch-$ELASTICSEARCH_VERSION" \
  c8run/custom_connectors \
  c8run/configuration \
  c8run/"camunda-zeebe-$CAMUNDA_VERSION"

