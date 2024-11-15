#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Set versions
CAMUNDA_VERSION="8.6.2"
ELASTICSEARCH_VERSION="8.13.4"
EXPECTED_JAVA_VERSION=21

# Set constants
BASEDIR=$(dirname "$0")
PARENTDIR=$(cd "$BASEDIR/.."; pwd)

PID_PATH=$BASEDIR/run.pid
ELASTIC_PID_PATH=$BASEDIR/elasticsearch.pid
CONNECTORS_PID_PATH=$BASEDIR/connectors.pid
POLLING_CAMUNDA_PID_PATH=$PARENTDIR/camunda-polling.pid

OPTIONS_HELP=$(cat <<EOF
Options:
  --config     - Applies the specified configuration file
  --detached   - Starts Camunda Run as a detached process
EOF
)

# Configuration file defaults overriden because upstream config doesn't export to elasticsearch
export ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_CLASSNAME="io.camunda.zeebe.exporter.ElasticsearchExporter"
export ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_URL="http://localhost:9200"
export ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_INDEX_PREFIX="zeebe-record"
export CAMUNDA_REST_QUERY_ENABLED=true
export CAMUNDA_OPERATE_CSRFPREVENTIONENABLED=false
export CAMUNDA_TASKLIST_CSRFPREVENTIONENABLED=false

# Set environment parameters
detachProcess=false

function stop {
  # Helper function to stop a process and remove its PID file
  stop_process() {
    local pid_path=$1
    local process_name=$2

    if [ -s "$pid_path" ]; then
      # Stop the process if it is still running
      kill $(cat "$pid_path")

      # Remove the process ID file
      rm "$pid_path"

      echo "$process_name is shutting down."
    fi
  }

  # Stop Camunda Run
  stop_process "$PID_PATH" "Camunda Run"

  # Stop Elasticsearch
  stop_process "$ELASTIC_PID_PATH" "Elasticsearch"

  # Stop Connectors
  stop_process "$CONNECTORS_PID_PATH" "Connectors"

  # Stop Polling Camunda
  stop_process "$POLLING_CAMUNDA_PID_PATH" "Polling Camunda"

  exit
}

function childExitHandler {
  status=$2
  if [[ "$status" == "0" ]]; then
    return
  fi

  if [[ "$status" == "1" ]]; then
    if [ -s "$POLLING_CAMUNDA_PID_PATH" ]; then
      polling_pid="$(cat "$POLLING_CAMUNDA_PID_PATH")"
      kill "$polling_pid"
      rm "$POLLING_CAMUNDA_PID_PATH"
    fi
  fi
}

trap stop SIGINT SIGTERM ERR
trap childExitHandler SIGCHLD

if [ "$1" = "start" ] ; then
  shift
  # setup the JVM
  if [ "x$JAVA_HOME" != "x" ]; then

    # Not all Java installations place the binary in `bin/java`, so use the `find` command.
    if [ "x$JAVA" = "x" ]; then # x-prefix to support legacy shells
      JAVA=$(find -L "$JAVA_HOME" -name "java" | head -n 1)
      echo Setting JAVA property to "$JAVA" in "$JAVA_HOME"
    fi
  else
    echo JAVA_HOME is not set. Unexpected results may occur.
    echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
    if [ "x$JAVA" = "x" ]; then # x-prefix to support legacy shells
      JAVA="java"
    fi
    # We want to set JAVA_HOME so that we can set elasticsearch to use JAVA_HOME
    export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v "$JAVA")")")")"
  fi

  JAVA_VERSION=$("$JAVA" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^0\./s///' | cut -d'.' -f1)
  # Check the Java version to ensure it meets the minimum required version
  echo "Java version is $("$JAVA" -version 2>&1 | head -1 | cut -d'"' -f2)"

  if [[ "$JAVA_VERSION" -lt "$EXPECTED_JAVA_VERSION" ]]; then
    echo "You must use at least JDK $EXPECTED_JAVA_VERSION to start Camunda Platform Run."
    childExitHandler $$ 1
    exit 1
  fi

  if [ "x$JAVA_OPTS" != "x" ]; then
    echo JAVA_OPTS: $JAVA_OPTS
  fi

  # Inspect arguments
  while [ "$1" != "" ]; do
    case $1 in
      --config )     shift
                     if [[ "$1" == "" ]]; then
                       printf "%s" "$OPTIONS_HELP"
                       exit 0
                     fi
                     configuration="$1"
                     ;;
      # the background flag shouldn't influence the optional component flags
      --detached )   detachProcess=true
                     echo Camunda Run will start in the background. Use the shutdown.sh script to stop it
                     ;;
      --help )       printf "%s" "$OPTIONS_HELP"
                     exit 0
                     ;;
      * )            exit 1
    esac
    shift
  done

  mkdir -p "$PARENTDIR/log"

  # limit the java heapspace used by ElasticSearch to 1GB
  export ES_JAVA_OPTS="-Xms1g -Xmx1g"
  export ES_JAVA_HOME="$JAVA_HOME"

  echo
  echo "Starting Elasticsearch ${ELASTICSEARCH_VERSION}...";
  echo "(Hint: you can find the log output in the 'elasticsearch.log' file in the 'log' folder of your distribution.)"
  echo
  ELASTICSEARCH_LOG_FILE=$PARENTDIR/log/elasticsearch.log
  "$PARENTDIR/elasticsearch-${ELASTICSEARCH_VERSION}/bin/elasticsearch" -E xpack.ml.enabled=false -E xpack.security.enabled=false </dev/null > "$ELASTICSEARCH_LOG_FILE" 2>&1 &
  echo $! > "$ELASTIC_PID_PATH"

  function checkStartup {
    RETRIES=20
      SLEEP_TIME=14
      URL=$1
      NAME=$2
      COMMAND="curl -XGET $URL"

    until ${COMMAND}&>/dev/null;
        do
          sleep ${SLEEP_TIME}
          RETRIES=$(( RETRIES - 1 ))
        if [ $RETRIES = 0 ]; then
          echo "Error: $NAME did not start!"
          exit 1;
        else
          echo "Waiting for $NAME to start. $RETRIES retries left"
        fi
      done
    echo "$NAME has successfully been started.";
  }

  URL="http://localhost:9200/_cluster/health?wait_for_status=green&wait_for_active_shards=all&wait_for_no_initializing_shards=true&timeout=120s"
  checkStartup "$URL" "Elasticsearch"

  # start the application
  if [[ "$configuration" != "" ]]; then
    if [[ "$configuration" == "/*" ]]; then
      extraArgs="--spring.config.location=classpath:$PARENTDIR/camunda-zeebe-$CAMUNDA_VERSION/config/application.yaml,classpath:$configuration"
    else
      extraArgs="--spring.config.location=classpath:$PARENTDIR/camunda-zeebe-$CAMUNDA_VERSION/config/application.yaml,classpath:$(pwd)/$configuration"
    fi
  fi

  if [ "$detachProcess" = "true" ]; then
    # check if a Camunda Run instance is already in operation
    if [ -s "$PID_PATH" ]; then
      echo "
A Camunda Run instance is already in operation (process id $(cat $PID_PATH)).

Please stop it or remove the file $PID_PATH."
      exit 1
    fi

    pushd "$PARENTDIR/camunda-zeebe-$CAMUNDA_VERSION/"
    echo ./bin/camunda $extraArgs >> "$PARENTDIR/log/camunda.log" 2>> "$PARENTDIR/log/camunda.log" &
    ./bin/camunda $extraArgs >> "$PARENTDIR/log/camunda.log" 2>> "$PARENTDIR/log/camunda.log" &
    echo $! > "$PID_PATH"
    popd

    "$JAVA" -cp "$PARENTDIR/*:$PARENTDIR/custom_connectors/*:./camunda-zeebe-$CAMUNDA_VERSION/lib/*" "io.camunda.connector.runtime.app.ConnectorRuntimeApplication" --spring.config.location=./connectors-application.properties >> "$PARENTDIR/log/connectors.log" 2>> "$PARENTDIR/log/connectors.log" &
    echo $! > "$CONNECTORS_PID_PATH"
    if [ -s "$POLLING_CAMUNDA_PID_PATH" ]; then
      wait "$(cat "$POLLING_CAMUNDA_PID_PATH")"
    fi
    cat endpoints.txt
  else
    "$JAVA" -cp "$PARENTDIR/*:$PARENTDIR/custom_connectors/*:./camunda-zeebe-$CAMUNDA_VERSION/lib/*" "io.camunda.connector.runtime.app.ConnectorRuntimeApplication" --spring.config.location=./connectors-application.properties >> "$PARENTDIR/log/connectors.log" 2>> "$PARENTDIR/log/connectors.log" &
    echo $! > "$CONNECTORS_PID_PATH"

    pushd "$PARENTDIR/camunda-zeebe-$CAMUNDA_VERSION/"
    echo ./bin/camunda $extraArgs 2>&1 | tee "$PARENTDIR/log/camunda.log"
    ./bin/camunda $extraArgs 2>&1 | tee "$PARENTDIR/log/camunda.log"
    popd
  fi
#   wait $(cat "$CONNECTORS_PID_PATH")
#   wait $(cat "$POLLING_CAMUNDA_PID_PATH")
#   wait $(cat "$ELASTIC_PID_PATH")
#   wait $(cat "$PID_PATH")

elif [ "$1" = "stop" ] ; then
  stop

elif [ "$1" = "" ] || [ "$1" = "help" ] ; then
  printf "Usage: run.sh [start|stop] (options...) \n%s" "$OPTIONS_HELP"
fi
