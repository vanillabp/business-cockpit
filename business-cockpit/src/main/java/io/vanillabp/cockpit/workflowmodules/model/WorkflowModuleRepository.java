package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowModuleRepository extends MongoRepository<WorkflowModule, String> {

	@Query("""
			{ '$or': [ \
			  { 'accessibleToGroups': { $in: ?0 } }, \
			  { 'accessibleToGroups': { $exists: false } }, \
			  { 'accessibleToGroups': { $size: 0 } } \
			] }""")
	List<WorkflowModule> findByAccessibleToGroups(List<String> groups);

	/**
	 * The same rule as {@link #findByAccessibleToGroups(List)} for one module, so that a module
	 * kept out of the list cannot be fetched by its id either.
	 */
	@Query("""
			{ '$and': [ \
			  { '_id': ?0 }, \
			  { '$or': [ \
			    { 'accessibleToGroups': { $in: ?1 } }, \
			    { 'accessibleToGroups': { $exists: false } }, \
			    { 'accessibleToGroups': { $size: 0 } } \
			  ] } \
			] }""")
	Optional<WorkflowModule> findByIdAndAccessibleToGroups(String id, List<String> groups);
}
