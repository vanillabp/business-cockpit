package io.vanillabp.cockpit.users.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the users of the {@code users} collection.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

}
