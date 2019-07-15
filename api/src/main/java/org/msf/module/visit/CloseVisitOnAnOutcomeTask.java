package org.msf.module.visit;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.openmrs.Concept;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.Date;
import java.util.List;

public class CloseVisitOnAnOutcomeTask extends AbstractTask {

    private VisitService visitService;
    private VisitCloseData visitCloseData;

    CloseVisitOnAnOutcomeTask() {
        super();
        visitService = Context.getVisitService();
        visitCloseData = new VisitCloseData();
    }

    @Override
    public void execute() {
        List<Visit> visits = visitService.getVisits(visitCloseData.getVisitTypes(), null, null,
                null, null, null, null, null, null, false, false);

        visits.forEach(openVisit -> {
            if (outcomeAvailable(openVisit, visitCloseData.getConcepts()))
                visitService.endVisit(openVisit, new Date());
        });
    }

    private boolean outcomeAvailable(Visit openVisit, List<Concept> concepts) {
        BahmniObsService bahmniObsService = Context.getService(BahmniObsService.class);
        return !bahmniObsService.getLatestObsByVisit(openVisit, concepts, null, true).isEmpty();
    }
}
