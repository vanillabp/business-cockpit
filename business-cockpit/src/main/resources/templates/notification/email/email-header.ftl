<#---
  Default subject template for user task notification e-mails.
  Available model: taskTitle, workflowTitle, taskDefinitionTitle, businessId,
  notificationTypeName, taskUri, taskListUri, userTask, userDetails, notificationType, forced.
--->
<#assign what>
<#if notificationTypeName == "CREATED">New user task<#elseif notificationTypeName == "CANDIDATE_USER">User task offered to you<#elseif notificationTypeName == "COMPLETED">User task completed<#elseif notificationTypeName == "CANCELED">User task cancelled<#else>User task notification</#if>
</#assign>
${what?trim}<#if taskTitle??>: ${taskTitle}</#if><#if businessId??> (${businessId})</#if>
