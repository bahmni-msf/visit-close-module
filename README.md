# Close Patient Visit
This module helps in closing the patient visit based on the observations filled.
# Build
For building this omod, run:
`mvn clean install`
# Deployment
Place that built omod in the following path:
`/opt/openmrs/modules`
# Description
It creates a new Scheduler Task `Close Visits On Outcome` which runs everyday at 11:50:00 pm. 

It also creates two new global properties `visits.closeOnAnOutcome.conceptName(s)` and `visits.closeOnAnOutcome.visitType(s)`.

<b>visits.closeOnAnOutcome.conceptName(s)</b> : We can configure the concepts here using `|` as separator. Based on their outcomes we can close the specified visits.

<b>Note</b>: To close visit on specific concept outcomes fully qualified concept name should be given in {"fully qualified concept name":["outcome1", "outcome2"]} format.

<b>Example</b>: concept name1 | {"concept name2":["outcome1", "outcome2"]} | concept name3

<b>visits.closeOnAnOutcome.visitType(s)</b> : Everytime when the scheduler runs the visits configured here will be closed, based on the outcomes of the concepts specified.
