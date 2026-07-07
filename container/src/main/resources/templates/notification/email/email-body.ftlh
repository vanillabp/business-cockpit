<#---
  Default body template for user task notification e-mails.
  Available model: taskTitle, workflowTitle, taskDefinitionTitle, businessId,
  notificationTypeName, baseUri, userTask, userDetails, notificationType, forced.
  The deep-link and task-list URLs are built here because their path segments are
  language-specific (this template is resolved per recipient locale; ".lang" is the
  language of that locale).
--->
<#assign taskPath = (.lang == "de")?then("aufgabe", "task")>
<#assign taskListPath = (.lang == "de")?then("aufgaben", "tasks")>
<html>
<body>
<h1>Hello ${userDetails.display!userDetails.id!"there"},</h1>

<p>
<#if notificationTypeName == "CREATED">
a new user task is available for you.
<#elseif notificationTypeName == "CANDIDATE_USER">
a user task has been offered to you personally.
<#elseif notificationTypeName == "COMPLETED">
a user task you had taken over was completed by someone else.
<#elseif notificationTypeName == "CANCELED">
a user task you had taken over was cancelled by the process.
<#else>
there is a change to a user task relevant to you.
</#if>
</p>

<pre>
<#if taskTitle??>Task: ${taskTitle}
</#if>
<#if workflowTitle??>Workflow: ${workflowTitle}
</#if>
<#if businessId??>Business ID: ${businessId}
</#if>
<#if forced>
Note: this notification was requested by the workflow module and cannot be switched off in your notification settings.
</#if>
</pre>

Open <a href="${baseUri}/${taskPath}/${userTask.id}">the task</a>.<br>
Your <a href="${baseUri}/${taskListPath}">task list</a>
</body>
</html>