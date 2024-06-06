#!/bin/sh -xeu

HOST=$(hostname -i)
ZEEBE_SET_SEED_ENV_VARS=${ZEEBE_SET_SEED_ENV_VARS:-true}

if [ "$ZEEBE_STANDALONE_GATEWAY" = "true" ]; then
    if [ "$ZEEBE_SET_SEED_ENV_VARS" = "true" ]; then
        export ZEEBE_GATEWAY_NETWORK_HOST=${ZEEBE_GATEWAY_NETWORK_HOST:-${HOST}}
    fi
    export ZEEBE_GATEWAY_CLUSTER_HOST=${ZEEBE_GATEWAY_CLUSTER_HOST:-${ZEEBE_GATEWAY_NETWORK_HOST}}

    exec /usr/local/zeebe/bin/gateway
elif [ "$ZEEBE_RESTORE" = "true" ]; then
    exec /usr/local/zeebe/bin/restore --backupId=${ZEEBE_RESTORE_FROM_BACKUP_ID}
else
    export ZEEBE_BROKER_NETWORK_HOST=${ZEEBE_BROKER_NETWORK_HOST:-${HOST}}
    export ZEEBE_BROKER_GATEWAY_CLUSTER_HOST=${ZEEBE_BROKER_GATEWAY_CLUSTER_HOST:-${ZEEBE_BROKER_NETWORK_HOST}}

    exec /usr/local/zeebe/bin/broker
fi
