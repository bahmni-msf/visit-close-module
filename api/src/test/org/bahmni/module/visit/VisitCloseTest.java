package org.bahmni.module.visit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.VisitType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.bahmni.module.visit.TestHelper.setValuesForMemberFields;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class VisitCloseTest {

    private VisitClose visitClose;
    @Mock
    private GlobalPropertyReader globalPropertyReader;
    @Mock
    private VisitService visitService;
    @Mock
    private ConceptService conceptService;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Context.class);
        when(Context.getVisitService()).thenReturn(visitService);
        when(Context.getConceptService()).thenReturn(conceptService);

        visitClose = new VisitClose();
        setValuesForMemberFields(visitClose, "globalPropertyReader", globalPropertyReader);
    }

    @Test
    public void shouldReturnVisitTypeObjectForGivenVisitTypeNameInGlobalProperty() {
        String visitTypeNames = "MLO, OPD";
        VisitType mloVisitType = mock(VisitType.class);
        VisitType opdVisitType = mock(VisitType.class);
        when(globalPropertyReader.getValueOfProperty("visits.closeOnAnOutcome.visitType(s)"))
                .thenReturn(visitTypeNames);
        when(visitService.getVisitTypes("MLO")).thenReturn(singletonList(mloVisitType));
        when(visitService.getVisitTypes("OPD")).thenReturn(singletonList(opdVisitType));


        List<VisitType> visitTypes = visitClose.getVisitTypes();

        assertEquals(2, visitTypes.size());
        assertTrue(visitTypes.containsAll(Arrays.asList(mloVisitType, opdVisitType)));

    }

    @Test
    public void shouldReturnEmptyListOfVisitTypesIfNoVisitProvidedInGlobalProperty() {
        when(globalPropertyReader.getValueOfProperty("visits.closeOnAnOutcome.visitType(s)"))
                .thenReturn(null);
        List<VisitType> visitTypes = visitClose.getVisitTypes();

        assertEquals(0, visitTypes.size());
    }

    @Test
    public void shouldConceptsForGivenConceptNamesInGlobalPropery() {
        String conceptNames = "FUP, Outcomes for follow-up surgical validation | FV, Outcomes FV";
        Concept concept1 = mock(Concept.class);
        Concept concept2 = mock(Concept.class);
        when(globalPropertyReader.getValueOfProperty("visits.closeOnAnOutcome.conceptName(s)"))
                .thenReturn(conceptNames);

        when(conceptService.getConcept("FUP, Outcomes for follow-up surgical validation"))
                .thenReturn(concept1);
        when(conceptService.getConcept("FV, Outcomes FV"))
                .thenReturn(concept2);


        List<Concept> concepts = visitClose.getConcepts();

        assertEquals(2, concepts.size());
        assertTrue(concepts.containsAll(Arrays.asList(concept1, concept2)));

    }
}
