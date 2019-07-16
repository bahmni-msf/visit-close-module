package org.msf.module.visit;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.openmrs.Concept;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class CloseVisitOnAnOutcomeTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(CloseVisitOnAnOutcomeTask.class);

    private VisitService visitService;
    private VisitCloseData visitCloseData;

    public CloseVisitOnAnOutcomeTask() {
        super();
        visitService = Context.getVisitService();
        visitCloseData = new VisitCloseData();
    }

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Starting patient visit close task based on concept outcomes...");
            }
            startExecuting();
            try {
                List<Visit> visits = visitService.getVisits(visitCloseData.getVisitTypes(), null, null,
                        null, null, null, null, null, null, false, false);

                visits.forEach(openVisit -> {
                    if (outcomeAvailable(openVisit, visitCloseData.getConcepts()))
                        visitService.endVisit(openVisit, new Date());
                });
            } catch (Exception e) {
                log.error("Error while closing patients visits based on concept outcomes...:", e);
            } finally {
                stopExecuting();
            }
        }

    }

    private boolean outcomeAvailable(Visit openVisit, List<Concept> concepts) {
        BahmniObsService bahmniObsService = Context.getService(BahmniObsService.class);
        return !bahmniObsService.getLatestObsByVisit(openVisit, concepts, null, true).isEmpty();
    }
}
