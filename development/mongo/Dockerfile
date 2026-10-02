# Pinned to the exact same point release as the server in docker-compose.yaml.
# Two reasons: (1) only mongosh is used here (as a client), and it ships with this
# image; (2) the floating "mongo:8.0" tag now resolves to a point release that
# refuses to run on Linux kernel >= 6.19 (SERVER-121912) - the Docker Desktop VM
# runs such a kernel. 8.0.4 predates that check.
# NOTE: this image must be rebuilt after a version bump - "docker compose up" alone
# reuses the cached image. Use "docker compose up --build" (or "docker compose build").
FROM mongo:8.0.4

CMD [ "bash", "-c", "chmod a+x /config/*.sh && /config/wait-for-it.sh business-cockpit-mongo:27017 -- /config/mongo-setup.sh"]
