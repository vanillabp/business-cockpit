FROM mongo:8.0

CMD [ "bash", "-c", "chmod a+x /config/*.sh && /config/wait-for-it.sh business-cockpit-mongo:27017 -- /config/mongo-setup.sh"]
