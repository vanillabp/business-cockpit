# Development

## MongoDB

Use

```sh
docker-compose up -d`
```

to start MongoDB. It is required to use MongoDB as a ReplicaSet, so one has to add this line to you `/etc/hosts` file:

```sh
127.0.0.1       business-cockpit-mongo
```

*Hint:* For Windows the file is `C:\Windows\System32\drivers\etc\hosts`.
