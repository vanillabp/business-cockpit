#!/usr/bin/env bash

PREVIOUS_HOSTNAME=`cat /config/mongo-init.flag`

if [ ! "|${PREVIOUS_HOSTNAME}|" = "|${HOSTNAME}|" ]; then
    echo "Init replicaset"
    mongo mongodb://business-cockpit-mongo:27017 /config/mongo-setup.js
    sleep 1
    echo "Create DB and user"
    mongo mongodb://business-cockpit-mongo:27017/business-cockpit /config/create-user.js
    echo "${HOSTNAME}" > /config/mongo-init.flag
else
    echo "Mongo already initialized"
fi
