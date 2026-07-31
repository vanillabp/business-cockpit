#!/usr/bin/env bash

# The legacy "mongo" shell was dropped with MongoDB 6; the images ship "mongosh" instead.

PREVIOUS_HOSTNAME=`cat /config/mongo-init.flag`

if [ ! "|${PREVIOUS_HOSTNAME}|" = "|${HOSTNAME}|" ]; then
    echo "Init replicaset"
    mongosh mongodb://business-cockpit-mongo:27017 /config/mongo-setup.js
    sleep 1
    echo "Create DB and user"
    mongosh mongodb://business-cockpit-mongo:27017/business-cockpit /config/create-user.js
    echo "${HOSTNAME}" > /config/mongo-init.flag
else
    echo "Mongo already initialized"
fi
