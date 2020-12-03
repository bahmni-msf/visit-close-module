package org.msf.module.visit;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bedmanagement.BedDetails;
import org.openmrs.module.bedmanagement.service.BedManagementService;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class CloseVisitOnAnOutcomeTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(CloseVisitOnAnOutcomeTask.class);
    final String HOSPITAL_VISIT_TYPE_AMMAN = "Hospital";

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
                if(isProgramStateConfiguredAndHasHospitalVisit()){
                    closeVisitForAmman(visits);
                }
                else {
                    visits.forEach(openVisit -> {
                        if (outcomeAvailable(openVisit, visitCloseData.getOutcomeConcepts()))
                            visitService.endVisit(openVisit, new Date());
                    });
                }
            } catch (Exception e) {
                log.error("Error while closing patients visits based on concept outcomes...:", e);
            } finally {
                stopExecuting();
            }
        }

    }

    private boolean isProgramStateConfiguredAndHasHospitalVisit() {
        return !visitCloseData.getProgramStateConcepts().isEmpty() && containsHospitalVisitType();
    }

    private boolean containsHospitalVisitType() {
        return visitCloseData.getVisitTypes().stream().anyMatch(visitType ->
                visitType.getName().equals(HOSPITAL_VISIT_TYPE_AMMAN));
    }

    private void closeVisitForAmman(List<Visit> visits) {
        for (Visit openVisit : visits) {
            ProgramWorkflowService programWorkflowService = Context.getService(ProgramWorkflowService.class);
            PatientProgram activePatientProgram = getActivePatientProgramForPatient(openVisit.getPatient(), programWorkflowService);
            if (activePatientProgram != null) {
                if (isHospitalVisit(openVisit)) {
                    if (isNotInAnyConfiguredProgramStateOrBedIsAssigned(programWorkflowService, activePatientProgram, openVisit.getPatient())) {
                        continue;
                    }
                } else {
                    if (!outcomeAvailable(openVisit, visitCloseData.getOutcomeConcepts()))
                        continue;
                }
            }
            visitService.endVisit(openVisit, new Date());
        }
    }

    private boolean outcomeAvailable(Visit openVisit, List<Concept> concepts) {
        BahmniObsService bahmniObsService = Context.getService(BahmniObsService.class);
        return !bahmniObsService.getLatestObsByVisit(openVisit, concepts, null, true).isEmpty();
    }

    private PatientProgram getActivePatientProgramForPatient(Patient patient, ProgramWorkflowService programWorkflowService) {
        List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(patient, null, null, null, null, null, false);
        PatientProgram activePatientProgram = null;
        for (PatientProgram patientProgram : patientPrograms) {
            if (patientProgram.getDateCompleted() == null) {
                activePatientProgram = patientProgram;
                break;
            }
        }
        return activePatientProgram;
    }

    private boolean isNotInAnyConfiguredProgramStateOrBedIsAssigned(ProgramWorkflowService programWorkflowService, PatientProgram activePatientProgram, Patient patient) {
       return visitCloseData.getProgramStateConcepts().stream().anyMatch(concept -> {
            List<ProgramWorkflowState> programWorkflowStatesByConcept = programWorkflowService.getProgramWorkflowStatesByConcept(concept);
            ProgramWorkflowState programWorkflowStateForNetWorkFollowUp = programWorkflowStatesByConcept.get(0);
            PatientState patientState = activePatientProgram.getCurrentState(null);
            BedManagementService bedManagementService = Context.getService(BedManagementService.class);
            ProgramWorkflowState patientCurrentWorkFlowState = patientState.getState();
            BedDetails bedAssignmentDetailsByPatient = bedManagementService.getBedAssignmentDetailsByPatient(patient);
            return !(patientCurrentWorkFlowState.equals(programWorkflowStateForNetWorkFollowUp) && patientState.getEndDate() == null && bedAssignmentDetailsByPatient == null);
        });
    }

    private boolean isHospitalVisit(Visit openVisit) {
        return openVisit.getVisitType().getName().equals(HOSPITAL_VISIT_TYPE_AMMAN);
    }
}
