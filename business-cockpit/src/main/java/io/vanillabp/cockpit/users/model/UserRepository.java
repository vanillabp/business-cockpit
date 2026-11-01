package io.vanillabp.cockpit.users.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the users of the {@code users} collection. A feature which changes one part of
 * a user's document uses the atomic writes of {@link UserDocumentWrites}, not {@code save}.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String>, UserDocumentWrites {

}
