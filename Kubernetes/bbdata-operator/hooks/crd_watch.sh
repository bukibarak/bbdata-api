#!/usr/bin/env bash

if [[ $1 == "--config" ]] ; then
  cat <<EOF
configVersion: v1
kubernetes:
- apiVersion: "stable.bukibarak.ch/v1"
  kind: BBData
  executeHookOnEvent:
  - Added
EOF
else
    type=$(jq -c -r '.[0].type' $BINDING_CONTEXT_PATH)
    specs=""

    # Get the CRD specs depending on the ressource type
    if [[ $type == "Synchronization" ]] ; then # In case the ressource watched is created before the script runs, might delete for production
      specs=$(jq -c -r '.[0].objects[0].object.spec' $BINDING_CONTEXT_PATH)
    elif [[ $type == "Event" ]] ; then
      specs=$(jq -c -r '.[0].object.spec' $BINDING_CONTEXT_PATH)
    fi

    if [[ $specs != "" && $specs != "null" ]] ; then
      # Set the env variables used in the subscripts
      export DEFAULT_REPLICA_SET=$(jq -c '.defaultReplica'  <<< $specs)
      if [[ $DEFAULT_REPLICA_SET == "null" ]] || ! [[ $DEFAULT_REPLICA_SET =~ ^[0-9]+$ ]] ; then
        export DEFAULT_REPLICA_SET=1
      fi

      export CASSANDRA_REPLICA_SET=$(jq -c '.cassandraReplica'  <<< $specs)
      if [[ $CASSANDRA_REPLICA_SET == "null" ]] || ! [[ $CASSANDRA_REPLICA_SET =~ ^[0-9]+$ ]] ; then
        export CASSANDRA_REPLICA_SET=$DEFAULT_REPLICA_SET
      fi

      export CASSANDRA_STORAGECLASS_NAME=$(jq -c '.cassandraStorageClass'  <<< $specs)
      if [[ $CASSANDRA_STORAGECLASS_NAME == "null" ]] ; then
        export CASSANDRA_STORAGECLASS_NAME=$(kubectl get sc -o "jsonpath={.items[0].metadata.name}")
      fi

      export KAFKA_REPLICA_SET=$(jq -c '.kafkaReplica'  <<< $specs)
      if [[ $KAFKA_REPLICA_SET == "null" ]] || ! [[ $KAFKA_REPLICA_SET =~ ^[0-9]+$ ]] ; then
        export KAFKA_REPLICA_SET=$DEFAULT_REPLICA_SET
      fi

      export BBDATA_REPLICA_SET=$(jq -c '.bbdataReplica'  <<< $specs)
      if [[ $BBDATA_REPLICA_SET == "null" ]] || ! [[ $BBDATA_REPLICA_SET =~ ^[0-9]+$ ]] ; then
        export BBDATA_REPLICA_SET=$DEFAULT_REPLICA_SET
      fi

      # Print the configuration to the console (for logs)
      echo "===== DEPLOYMENT CONFIGURATION ====="
      echo "DEFAULT REPLICA:   $DEFAULT_REPLICA_SET"
      echo "CASSANDRA REPLICA: $CASSANDRA_REPLICA_SET"
      echo "KAFKA REPLICA:     $KAFKA_REPLICA_SET"
      echo "BBDATA REPLICA:    $BBDATA_REPLICA_SET"
      echo "CASSANDRA SC:      $CASSANDRA_STORAGECLASS_NAME"

      # Get the path of the scripts folder
      SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )/../scripts"

      # Call all the scripts and exit in case of failure
      $SCRIPTPATH/cassandra.sh
      if [[ $? -eq 1 ]] ; then
        exit 0
      fi

      $SCRIPTPATH/kafka.sh
      $SCRIPTPATH/mysql.sh
      $SCRIPTPATH/spring.sh
      $SCRIPTPATH/node.sh
    fi
fi