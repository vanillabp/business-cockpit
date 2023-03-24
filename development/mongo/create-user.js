db.createUser({
    user: 'business-cockpit',
    pwd: 'business-cockpit',
    roles: [
      {
        role:'dbOwner',
        db: 'business-cockpit'
      }
    ]
  });
