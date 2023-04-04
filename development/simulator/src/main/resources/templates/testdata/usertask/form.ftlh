<html>
<body>

<h1>Generate user-tasks in business-cockpit.</h1>

<form action="/testdata/usertask/generate" method="POST">
<p><input name="noOfEvents" value="10"> &nbsp; The number of events</p>
<p><input name="languages" value="en,de"> &nbsp; The languages for which to generate titles.</p>
<p><input name="percentageUserCandidates" value="50"> &nbsp; The amount of tasks having a user candidate (in percent).</p>
<p><input name="percentageGroupCandidates" value="50"> &nbsp; The amount of tasks having a group candidate (in percent).</p>
<p><input name="percentageUserAssignments" value="50"> &nbsp; The amount of tasks assigned to a user (in percent).</p>
<p><input name="percentageUpdates" value="0"> &nbsp; The amount of tasks which will be updated.</p>
<p><input name="noOfConcurrentRequest" value="1"> &nbsp; The number of concurrent requests sent to business-cockpit.</p>
<p><input name="noOfUsers" value="2"> &nbsp; The number of users to assign tasks to.</p>
<p><input name="noOfGroups" value="1"> &nbsp; The number of groups to assign tasks to.</p>
<p><input name="throttling" value="10"> &nbsp; The throttling of task generation in milliseconds.</p>
<p><input type="submit" value="Generate"></p>
</form>

</body>
</html>
