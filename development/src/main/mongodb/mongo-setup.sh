#!/usr/bin/env bash

if [ ! -f /data/mongo-init.flag ]; then
    echo "Init replicaset"
    mongo mongodb://business-cockpit-mongo:27017 mongo-setup.js
    sleep 1
    echo "Create DB and user"
    mongo --authenticationDatabase admin mongodb://business-cockpit-mongo:27017/bc create-user.js
    touch /data/mongo-init.flag
else
    echo "Replicaset already initialized"
fi
