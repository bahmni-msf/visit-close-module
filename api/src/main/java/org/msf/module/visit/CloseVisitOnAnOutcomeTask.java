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

import java.util.ArrayList;
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
            List<PatientProgram> activePatientProgramList = getActivePatientProgramForPatient(openVisit.getPatient(), programWorkflowService);
            if (!activePatientProgramList.isEmpty()) {
                if (isHospitalVisit(openVisit)) {
                    if (isNotInAnyConfiguredProgramStateOrBedIsAssigned(programWorkflowService, activePatientProgramList, openVisit.getPatient())) {
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

    private boolean isNotInAnyConfiguredProgramStateOrBedIsAssigned(ProgramWorkflowService programWorkflowService, List<PatientProgram> activePatientProgramList, Patient patient) {
        if(activePatientProgramList.size() > 1){
            int count = 0;
            for(PatientProgram patientProgram : activePatientProgramList){
                if(isNotInAnyConfiguredProgramState(programWorkflowService, patientProgram,patient)) {
                    count ++;
                }
               }
            if(count == 2){
                return isBedAssigned(patient);
            }
            return true;
        }
        else {
            return (isNotInAnyConfiguredProgramState(programWorkflowService, activePatientProgramList.get(0),patient) || isBedAssigned(patient));
        }
    }

    private boolean outcomeAvailable(Visit openVisit, List<Concept> concepts) {
        BahmniObsService bahmniObsService = Context.getService(BahmniObsService.class);
        return !bahmniObsService.getLatestObsByVisit(openVisit, concepts, null, true).isEmpty();
    }

    private List<PatientProgram> getActivePatientProgramForPatient(Patient patient, ProgramWorkflowService programWorkflowService) {
        List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(patient, null, null, null, null, null, false);
        List<PatientProgram> activePatientProgramList = new ArrayList<>();
        for (PatientProgram patientProgram : patientPrograms) {
            if (patientProgram.getDateCompleted() == null) {
                activePatientProgramList.add(patientProgram);
            }
        }
        return activePatientProgramList;
    }

    private boolean isNotInAnyConfiguredProgramState(ProgramWorkflowService programWorkflowService, PatientProgram activePatientProgram, Patient patient) {
       return visitCloseData.getProgramStateConcepts().stream().anyMatch(concept -> {
            List<ProgramWorkflowState> programWorkflowStatesByConcept = programWorkflowService.getProgramWorkflowStatesByConcept(concept);
            ProgramWorkflowState programWorkflowStateForNetWorkFollowUp = programWorkflowStatesByConcept.get(0);
            PatientState patientState = activePatientProgram.getCurrentState(null);
            ProgramWorkflowState patientCurrentWorkFlowState = patientState.getState();
            return !(patientCurrentWorkFlowState.equals(programWorkflowStateForNetWorkFollowUp) && patientState.getEndDate() == null);
        });
    }

    private boolean isBedAssigned(Patient patient){
        BedManagementService bedManagementService = Context.getService(BedManagementService.class);
        BedDetails bedAssignmentDetailsByPatient = bedManagementService.getBedAssignmentDetailsByPatient(patient);
        return !(bedAssignmentDetailsByPatient == null);
    }

    private boolean isHospitalVisit(Visit openVisit) {
        return openVisit.getVisitType().getName().equals(HOSPITAL_VISIT_TYPE_AMMAN);
    }
}
