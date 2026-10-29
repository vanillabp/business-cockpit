package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the documents of the registered workflow modules.
 */
@Repository
public interface WorkflowModuleRepository extends MongoRepository<WorkflowModule, String> {

	/**
	 * The modules registered for one of the given groups, plus the modules which registered no
	 * groups at all and so are open to everybody.
	 *
	 * @param groups The groups of the user
	 * @return The modules these groups let through
	 */
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
	 *
	 * @param id The id of the workflow module
	 * @param groups The groups of the user
	 * @return The module, or empty when there is none or the groups do not let it through
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
