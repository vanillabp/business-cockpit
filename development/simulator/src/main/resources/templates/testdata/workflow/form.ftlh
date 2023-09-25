<html>
<body>

<h1>Generate workflows in business-cockpit.</h1>

<form action="/testdata/workflow/generate" method="POST">
<p><input name="noOfEvents" value="10"> &nbsp; The number of events</p>
<p><input name="languages" value="en,de"> &nbsp; The languages for which to generate titles.</p>
<p><input name="percentageUpdates" value="0"> &nbsp; The amount of workflows which will be updated.</p>
<p><input name="noOfConcurrentRequest" value="1"> &nbsp; The number of concurrent requests sent to business-cockpit.</p>
<p><input name="throttling" value="10"> &nbsp; The throttling of workflow generation in milliseconds.</p>
<p><input type="submit" value="Generate"></p>
</form>

</body>
</html>
