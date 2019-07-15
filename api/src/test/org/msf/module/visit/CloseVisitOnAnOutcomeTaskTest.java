package org.msf.module.visit;

import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.msf.module.visit.TestHelper.setValuesForMemberFields;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class CloseVisitOnAnOutcomeTaskTest {

    @Mock
    private VisitService visitService;

    @Mock
    private VisitCloseData visitCloseData;

    private CloseVisitOnAnOutcomeTask closeVisitOnAnOutcomeTask;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Context.class);
        when(Context.getVisitService()).thenReturn(visitService);

        closeVisitOnAnOutcomeTask = new CloseVisitOnAnOutcomeTask();

        setValuesForMemberFields(closeVisitOnAnOutcomeTask, "visitCloseData", visitCloseData);

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
        when(visitCloseData.getConcepts()).thenReturn(concepts);
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
        when(visitCloseData.getConcepts()).thenReturn(concepts);
        when(Context.getService(BahmniObsService.class)).thenReturn(obsService);
        when(obsService.getLatestObsByVisit(any(Visit.class), anyCollectionOf(Concept.class), any(), any()))
                .thenReturn(Collections.emptyList());

        closeVisitOnAnOutcomeTask.execute();

        verify(visitService, times(0)).endVisit(any(Visit.class), any(Date.class));
    }
}
