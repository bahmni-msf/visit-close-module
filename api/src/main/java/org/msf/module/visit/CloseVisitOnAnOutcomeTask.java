package org.msf.module.visit;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bedmanagement.BedDetails;
import org.openmrs.module.bedmanagement.service.BedManagementService;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CloseVisitOnAnOutcomeTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(CloseVisitOnAnOutcomeTask.class);
    final String HOSPITAL_VISIT_TYPE_AMMAN = "Hospital";

    private ConceptService conceptService;
    private VisitService visitService;
    private VisitCloseData visitCloseData;

    public CloseVisitOnAnOutcomeTask() {
        super();
        conceptService = Context.getConceptService();
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
        if (activePatientProgramList.size() > 1) {
            return isNotInAnyConfiguredProgramStatesWithMultipleProgramsOrBedAssigned(programWorkflowService, activePatientProgramList, patient);
        } else {
            return (isNotInAnyConfiguredProgramState(programWorkflowService, activePatientProgramList.get(0)) || isBedAssigned(patient));
        }
    }

    private boolean isNotInAnyConfiguredProgramStatesWithMultipleProgramsOrBedAssigned(ProgramWorkflowService programWorkflowService, List<PatientProgram> activePatientProgramList, Patient patient) {
        int activeProgramsWithConfiguredState = 0;
        for (PatientProgram patientProgram : activePatientProgramList) {
            if (!isNotInAnyConfiguredProgramState(programWorkflowService, patientProgram)) {
                activeProgramsWithConfiguredState++;
            }
        }
        if (activeProgramsWithConfiguredState == activePatientProgramList.size()) {
            return isBedAssigned(patient);
        }
        return true;
    }

    private boolean isValidJsonString(String jsonString) {
        JSONObject json = (JSONObject) JSONValue.parse(jsonString);
        return !(null == json);
    }

    private List<Concept> getConceptsWithoutAnswers(String[] concepts) {
        String[] conceptsWithoutAnswers = Arrays.stream(concepts).filter(concept -> !isValidJsonString(concept)).toArray(size -> new String[size]);
        return Arrays.stream(conceptsWithoutAnswers).map(conceptService::getConcept).collect(Collectors.toList());
    }

    private List<JSONObject> getConceptsWithAnswers(String[] concepts) {
        String[] conceptsWithAnswers = Arrays.stream(concepts).filter(concept -> isValidJsonString(concept)).toArray(size -> new String[size]);
        return Arrays.stream(conceptsWithAnswers).map(concept -> (JSONObject) JSONValue.parse(concept)).collect(Collectors.toList());
    }

    private boolean specificOutcomeAvailable(Visit openVisit, JSONObject conceptWithAnswers) {
        BahmniObsService bahmniObsService = Context.getService(BahmniObsService.class);
        String questionConceptStr = (String) conceptWithAnswers.keySet().iterator().next();
        Concept questionConcept = conceptService.getConcept(questionConceptStr);
        List<String> answerConcepts = (JSONArray) conceptWithAnswers.get(questionConceptStr);
        Collection<BahmniObservation> obs = bahmniObsService.getLatestObsByVisit(openVisit, Arrays.asList(questionConcept), null, true);
        Iterator<BahmniObservation> obsIterator = obs.iterator();
        while (obsIterator.hasNext()) {
            BahmniObservation ob = obsIterator.next();
            if(answerConcepts.contains(ob.getValueAsString())) {
                return true;
            }
        }
        return false;
    }

    private boolean outcomeAvailable(Visit openVisit, String[] concepts) {
        List<Concept> conceptsWithoutAnswers = getConceptsWithoutAnswers(concepts);
        BahmniObsService bahmniObsService = Context.getService(BahmniObsService.class);
        if (bahmniObsService.getLatestObsByVisit(openVisit, conceptsWithoutAnswers, null, true).isEmpty()) {
            List<JSONObject> conceptsWithAnswers = getConceptsWithAnswers(concepts);
            for (JSONObject conceptWithAnswers: conceptsWithAnswers) {
                if (specificOutcomeAvailable(openVisit, conceptWithAnswers)) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
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

    private boolean isNotInAnyConfiguredProgramState(ProgramWorkflowService programWorkflowService, PatientProgram activePatientProgram) {
        return visitCloseData.getProgramStateConcepts().stream().noneMatch(concept -> {
            List<ProgramWorkflowState> programWorkflowStatesByConcept = programWorkflowService.getProgramWorkflowStatesByConcept(concept);
            ProgramWorkflowState programWorkFlowStateForConfiguredConcept = programWorkflowStatesByConcept.get(0);
            PatientState patientState = activePatientProgram.getCurrentState(null);
            ProgramWorkflowState patientCurrentWorkFlowState = patientState.getState();
            return patientCurrentWorkFlowState.equals(programWorkFlowStateForConfiguredConcept) && patientState.getEndDate() == null;
        });
    }

    private boolean isBedAssigned(Patient patient){
        BedManagementService bedManagementService = Context.getService(BedManagementService.class);
        BedDetails bedAssignmentDetailsByPatient = bedManagementService.getBedAssignmentDetailsByPatient(patient);
        return bedAssignmentDetailsByPatient != null;
    }

    private boolean isHospitalVisit(Visit openVisit) {
        return openVisit.getVisitType().getName().equals(HOSPITAL_VISIT_TYPE_AMMAN);
    }
}
