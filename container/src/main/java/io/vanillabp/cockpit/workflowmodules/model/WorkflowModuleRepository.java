package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;

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
}
