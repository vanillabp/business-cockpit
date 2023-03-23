FROM mongo:4.4

RUN mkdir /config
WORKDIR /config
COPY wait-for-it.sh .
COPY mongo-setup.js .
COPY mongo-setup.sh .
COPY create-user.js .
RUN chmod +x /config/wait-for-it.sh
RUN chmod +x /config/mongo-setup.sh
CMD [ "bash", "-c", "/config/wait-for-it.sh business-cockpit-mongo:27017 -- /config/mongo-setup.sh"]
