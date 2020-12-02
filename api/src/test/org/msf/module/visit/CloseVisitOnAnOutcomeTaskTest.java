package org.msf.module.visit;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.PatientProgram;
import org.openmrs.ConceptName;
import org.openmrs.Patient;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bedmanagement.BedDetails;
import org.openmrs.module.bedmanagement.service.BedManagementService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.ArrayList;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.msf.module.visit.TestHelper.setValueForFinalStaticField;
import static org.msf.module.visit.TestHelper.setValuesForMemberFields;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class CloseVisitOnAnOutcomeTaskTest {

    @Mock
    private VisitService visitService;

    @Mock
    private VisitCloseData visitCloseData;

    @Mock
    private Logger log;

    @Mock
    private ProgramWorkflowService programWorkflowService;

    @Mock
    private BedManagementService bedManagementService;

    @Mock
    private PatientProgram activePatientProgram;

    @Mock
    private ConceptService conceptService;

    @Mock
    private BahmniObsService bahmniObsService;

    private Concept networkFollowupConcept;

    private CloseVisitOnAnOutcomeTask closeVisitOnAnOutcomeTask;

    private Concept setUpConceptData(Integer conceptId, String name) {
        Concept concept = new Concept();
        concept.setId(conceptId);
        ConceptName conceptName = new ConceptName();
        conceptName.setName(name);
        conceptName.setLocale(new Locale("en", "GB"));
        concept.setNames(Arrays.asList(conceptName));
        return concept;
    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Context.class);
        when(Context.getVisitService()).thenReturn(visitService);

        closeVisitOnAnOutcomeTask = new CloseVisitOnAnOutcomeTask();

        setValuesForMemberFields(closeVisitOnAnOutcomeTask, "visitCloseData", visitCloseData);
        setValueForFinalStaticField(CloseVisitOnAnOutcomeTask.class, "log", log);

        networkFollowupConcept = setUpConceptData(103, "Network Follow-up");
    }

    @Test
    public void shouldVerifyEndVisitIsCalledWhenAnyObsAvailableForGivenConcepts() {
        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);
        Visit visit = mock(Visit.class);
        Concept concept = mock(Concept.class);
        List<Concept> concepts = singletonList(concept);
        BahmniObsService obsService = mock(BahmniObsService.class);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class)))
                .thenReturn(singletonList(visit));
        when(visitCloseData.getOutcomeConcepts()).thenReturn(concepts);
        when(Context.getService(BahmniObsService.class)).thenReturn(obsService);
        when(obsService.getLatestObsByVisit(any(Visit.class), anyCollectionOf(Concept.class), any(), any()))
                .thenReturn(singletonList(null));

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService).endVisit(any(Visit.class), any(Date.class));
    }

    @Test
    public void shouldVerifyEndVisitIsNotCalledWhenAnyObsAvailableForGivenConcepts() {
        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);
        Visit visit = mock(Visit.class);
        Concept concept = mock(Concept.class);
        List<Concept> concepts = singletonList(concept);
        BahmniObsService obsService = mock(BahmniObsService.class);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class)))
                .thenReturn(singletonList(visit));
        when(visitCloseData.getOutcomeConcepts()).thenReturn(concepts);
        when(Context.getService(BahmniObsService.class)).thenReturn(obsService);
        when(obsService.getLatestObsByVisit(any(Visit.class), anyCollectionOf(Concept.class), any(), any()))
                .thenReturn(Collections.emptyList());

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(0)).endVisit(any(Visit.class), any(Date.class));
    }

    @Test
    public void shouldVerifyEndVisitIsNotCalledWhenisExecutingFieldIsTrue() {

        closeVisitOnAnOutcomeTask.startExecuting();

        closeVisitOnAnOutcomeTask.execute();
        verify(visitService, times(0)).endVisit(any(Visit.class), any(Date.class));

    }

    @Test
    public void shouldLogErrorOnAnyExceptionWhileExecutingTheTask() {
        when(visitCloseData.getVisitTypes()).thenThrow(InvalidParameterException.class);

        closeVisitOnAnOutcomeTask.execute();

        verify(log).error(eq("Error while closing patients visits based on concept outcomes...:"), any(InvalidParameterException.class));
    }

    @Test
    public void shouldCloseTheVisitWhenPatientDoesNotHaveAnyActiveProgramsInAmman() {
        Visit visit = new Visit(1000);
        visit.setPatient(new Patient(2));

        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);

        Concept concept = mock(Concept.class);

        PatientProgram patientProgram = new PatientProgram();
        patientProgram.setId(1234);
        patientProgram.setDateCompleted(new Date());
        List<PatientProgram> patientPrograms = new ArrayList<>();
        patientPrograms.add(patientProgram);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class)))
                .thenReturn(singletonList(visit));
        when(visitCloseData.getProgramStateConcepts()).thenReturn(singletonList(concept));
        when(Context.getService(ProgramWorkflowService.class)).thenReturn(programWorkflowService);
        when(programWorkflowService.getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false))).thenReturn(patientPrograms);

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(1)).getVisits(anyCollectionOf(VisitType.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), eq(false));
        verify(programWorkflowService, times(1)).getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false));
        verify(visitService, times(1)).endVisit(eq(visit), isA(Date.class));
    }

    @Test
    public void shouldCloseTheVisitWhenThatVisitIsHospitalVisitAndProgramInNetworkFollowupStateAndHasNoBedAssignedForThePatientInAmman() {
        Patient patient = new Patient();
        patient.setUuid("Uuid");

        Concept concept = mock(Concept.class);

        PatientState patientState = new PatientState();
        patientState.setId(1);
        patientState.setPatientProgram(new PatientProgram());

        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);

        List<ProgramWorkflowState> programWorkflowStates = new ArrayList<>();
        ProgramWorkflowState networkFollowUpProgramWorkflowState = new ProgramWorkflowState(1);
        networkFollowUpProgramWorkflowState.setName("Something");
        networkFollowUpProgramWorkflowState.setConcept(networkFollowupConcept);
        programWorkflowStates.add(networkFollowUpProgramWorkflowState);
        patientState.setState(networkFollowUpProgramWorkflowState);

        Visit hospitalVisit = new Visit(1001);
        hospitalVisit.setVisitType(new VisitType("Hospital", "visit"));
        hospitalVisit.setPatient(patient);

        List<PatientProgram> patientPrograms = new ArrayList<>();
        patientPrograms.add(activePatientProgram);

        List<Visit> visits = new ArrayList<>();
        visits.add(hospitalVisit);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class))).thenReturn(visits);
        when(visitCloseData.getProgramStateConcepts()).thenReturn(singletonList(concept));
        when(Context.getService(ProgramWorkflowService.class)).thenReturn(programWorkflowService);
        when(Context.getConceptService()).thenReturn(conceptService);
        when(conceptService.getConcept("Network Follow-up")).thenReturn(networkFollowupConcept);
        when(programWorkflowService.getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false))).thenReturn(patientPrograms);
        when(programWorkflowService.getProgramWorkflowStatesByConcept(networkFollowupConcept)).thenReturn(programWorkflowStates);
        when(Context.getService(BedManagementService.class)).thenReturn(bedManagementService);
        when(bedManagementService.getBedAssignmentDetailsByPatient(patient)).thenReturn(null);
        when(activePatientProgram.getDateCompleted()).thenReturn(null);
        when(activePatientProgram.getCurrentState(null)).thenReturn(patientState);

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(1)).getVisits(anyCollectionOf(VisitType.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), eq(false));
        verify(programWorkflowService, times(1)).getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false));
        verify(visitService, times(1)).endVisit(eq(hospitalVisit), isA(Date.class));
    }

    @Test
    public void shouldNotCloseTheHospitalVisitWhenBedIsAssignedAndStateIsNetworkFollowupInAmman() {
        BedDetails bedDetails = new BedDetails();
        Patient patient = new Patient();
        patient.setUuid("Uuid");

        PatientState patientState = new PatientState();
        patientState.setId(1);
        patientState.setPatientProgram(new PatientProgram());

        Concept concept = mock(Concept.class);

        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);

        List<ProgramWorkflowState> programWorkflowStates = new ArrayList<>();
        ProgramWorkflowState networkFollowUpProgramWorkflowState = new ProgramWorkflowState(1);
        networkFollowUpProgramWorkflowState.setName("Something");
        networkFollowUpProgramWorkflowState.setConcept(networkFollowupConcept);
        programWorkflowStates.add(networkFollowUpProgramWorkflowState);
        patientState.setState(networkFollowUpProgramWorkflowState);

        Visit hospitalVisit = new Visit(1001);
        hospitalVisit.setVisitType(new VisitType("Hospital", "visit"));
        hospitalVisit.setPatient(patient);

        List<PatientProgram> patientPrograms = new ArrayList<>();
        patientPrograms.add(activePatientProgram);

        List<Visit> visits = new ArrayList<>();
        visits.add(hospitalVisit);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitCloseData.getProgramStateConcepts()).thenReturn(singletonList(concept));

        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class))).thenReturn(visits);
        when(Context.getService(ProgramWorkflowService.class)).thenReturn(programWorkflowService);
        when(conceptService.getConcept("Network Follow-up")).thenReturn(networkFollowupConcept);

        when(programWorkflowService.getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false))).thenReturn(patientPrograms);
        when(programWorkflowService.getProgramWorkflowStatesByConcept(networkFollowupConcept)).thenReturn(programWorkflowStates);
        when(Context.getService(BedManagementService.class)).thenReturn(bedManagementService);

        when(bedManagementService.getBedAssignmentDetailsByPatient(patient)).thenReturn(bedDetails);
        when(activePatientProgram.getDateCompleted()).thenReturn(null);
        when(activePatientProgram.getCurrentState(null)).thenReturn(patientState);

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(1)).getVisits(anyCollectionOf(VisitType.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), eq(false));
        verify(programWorkflowService, times(1)).getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false));
        verify(visitService, times(0)).endVisit(eq(hospitalVisit), isA(Date.class));
    }

    @Test
    public void shouldNotCloseVisitWhenVisitIsHospitalAndWhenTheStateIsNotNetworkFollowupInAmman() {
        Patient patient = new Patient();
        patient.setUuid("Uuid");

        Concept concept = mock(Concept.class);

        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);

        PatientState patientState = new PatientState();
        patientState.setId(1);
        patientState.setPatientProgram(new PatientProgram());

        List<ProgramWorkflowState> programWorkflowStates = new ArrayList<>();
        ProgramWorkflowState networkFollowUpProgramWorkflowState = new ProgramWorkflowState(1);
        networkFollowUpProgramWorkflowState.setName("Something");
        networkFollowUpProgramWorkflowState.setConcept(networkFollowupConcept);
        programWorkflowStates.add(networkFollowUpProgramWorkflowState);
        patientState.setState(new ProgramWorkflowState());

        Visit hospitalVisit = new Visit(1001);
        hospitalVisit.setVisitType(new VisitType("Hospital", "visit"));
        hospitalVisit.setPatient(patient);

        List<PatientProgram> patientPrograms = new ArrayList<>();
        patientPrograms.add(activePatientProgram);

        List<Visit> visits = new ArrayList<>();
        visits.add(hospitalVisit);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitCloseData.getProgramStateConcepts()).thenReturn(singletonList(concept));
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class))).thenReturn(visits);

        when(Context.getService(ProgramWorkflowService.class)).thenReturn(programWorkflowService);
        when(conceptService.getConcept("Network Follow-up")).thenReturn(networkFollowupConcept);

        when(programWorkflowService.getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false))).thenReturn(patientPrograms);
        when(programWorkflowService.getProgramWorkflowStatesByConcept(networkFollowupConcept)).thenReturn(programWorkflowStates);

        when(Context.getService(BedManagementService.class)).thenReturn(bedManagementService);
        when(bedManagementService.getBedAssignmentDetailsByPatient(patient)).thenReturn(null);
        when(activePatientProgram.getDateCompleted()).thenReturn(null);
        when(activePatientProgram.getCurrentState(null)).thenReturn(patientState);

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(1)).getVisits(anyCollectionOf(VisitType.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), eq(false));
        verify(programWorkflowService, times(1)).getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false));
        verify(visitService, times(0)).endVisit(eq(hospitalVisit), isA(Date.class));

    }
    @Test
    public void shouldCloseTheVisitWhenOutcomesAreFilledAndIsNotAHospitalVisitInAmman() {
        Patient patient = new Patient();
        patient.setUuid("Uuid");
        Visit openVisit = new Visit();
        openVisit.setPatient(patient);
        openVisit.setVisitType(new VisitType("some", "visit"));
        List<Visit> visits = new ArrayList<>();
        visits.add(openVisit);

        Concept concept = mock(Concept.class);

        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);

        PatientProgram patientProgram = new PatientProgram();
        patientProgram.setId(1234);
        patientProgram.setDateCompleted(null);
        List<PatientProgram> patientPrograms = new ArrayList<>();
        patientPrograms.add(patientProgram);
        Collection<BahmniObservation> bahmniObservations = new ArrayList<>();
        bahmniObservations.add(new BahmniObservation());


        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitCloseData.getProgramStateConcepts()).thenReturn(singletonList(concept));
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class))).thenReturn(visits);

        when(Context.getService(ProgramWorkflowService.class)).thenReturn(programWorkflowService);
        when(visitCloseData.getOutcomeConcepts()).thenReturn(singletonList(networkFollowupConcept));
        when(Context.getService(BahmniObsService.class)).thenReturn(bahmniObsService);
        when(bahmniObsService.getLatestObsByVisit(eq(openVisit), any(Collection.class), eq(null), eq(true))).thenReturn(bahmniObservations);
        when(conceptService.getConcept("Network Follow-up")).thenReturn(networkFollowupConcept);
        when(programWorkflowService.getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false))).thenReturn(patientPrograms);

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(1)).getVisits(anyCollectionOf(VisitType.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), eq(false));
        verify(programWorkflowService, times(1)).getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false));
        verify(visitService, times(1)).endVisit(eq(openVisit), isA(Date.class));

    }

    @Test
    public void shouldNotCloseTheVisitWhenOutcomeConceptsAreNotFilledInAmman() {
        Patient patient = new Patient();
        patient.setUuid("Uuid");
        Visit openVisit = new Visit();
        openVisit.setPatient(patient);
        openVisit.setVisitType(new VisitType("some", "visit"));
        List<Visit> visits = new ArrayList<>();
        visits.add(openVisit);

        PatientProgram patientProgram = new PatientProgram();
        patientProgram.setId(1234);
        patientProgram.setDateCompleted(null);
        List<PatientProgram> patientPrograms = new ArrayList<>();
        patientPrograms.add(patientProgram);

        Concept concept = mock(Concept.class);

        VisitType visitType = mock(VisitType.class);
        List<VisitType> visitTypes = singletonList(visitType);

        when(visitCloseData.getVisitTypes()).thenReturn(visitTypes);
        when(visitCloseData.getProgramStateConcepts()).thenReturn(singletonList(concept));
        when(visitService.getVisits(anyCollectionOf(VisitType.class), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class))).thenReturn(visits);
        when(Context.getService(ProgramWorkflowService.class)).thenReturn(programWorkflowService);
        when(programWorkflowService.getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false))).thenReturn(patientPrograms);

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(1)).getVisits(anyCollectionOf(VisitType.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), eq(false));
        verify(programWorkflowService, times(1)).getPatientPrograms(isA(Patient.class), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false));
        verify(visitService, times(0)).endVisit(eq(openVisit), isA(Date.class));
    }

}
