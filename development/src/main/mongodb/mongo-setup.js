rsconf = {
    _id : "rs0",
    members: [
        {
            "_id": 0,
            "host": "business-cockpit-mongo:27017",
            "priority": 3
        }
    ]
}

rs.initiate(rsconf);
