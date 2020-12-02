package org.msf.module.visit;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.VisitType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class VisitCloseData {

    private static final String VISIT_TYPES_GLOBAL_PROPERTY = "visits.closeOnAnOutcome.visitType(s)";
    private static final String CONCEPTS_GLOBAL_PROPERTY = "visits.closeOnAnOutcome.conceptName(s)";
    private static final String PROGRAM_STATE_GLOBAL_PROPERTY = "visits.closeOnAnOutcome.programState";
    private GlobalPropertyReader globalPropertyReader;
    private VisitService visitService;
    private ConceptService conceptService;

    VisitCloseData() {
        this.globalPropertyReader = new GlobalPropertyReader();
        this.visitService = Context.getVisitService();
        this.conceptService = Context.getConceptService();
    }

    List<VisitType> getVisitTypes() {
        ArrayList<VisitType> visitTypes = new ArrayList<>();
        String commaSeparatedVisitTypes = globalPropertyReader.getValueOfProperty(VISIT_TYPES_GLOBAL_PROPERTY);
        if (StringUtils.isBlank(commaSeparatedVisitTypes))
            return visitTypes;
        String[] visitTypeNames = commaSeparatedVisitTypes.split("\\s*,\\s*");
        for (String visitTypeName : visitTypeNames) {
            List<VisitType> matchedVisitTypes = visitService.getVisitTypes(visitTypeName);
            visitTypes.addAll(matchedVisitTypes);
        }
        return visitTypes;
    }

    private List<Concept> getConcepts(String globalPropertyType) {
        ArrayList<Concept> concepts = new ArrayList<>();
        String pipeSeparatedConceptNames = globalPropertyReader.getValueOfProperty(globalPropertyType);
        if (StringUtils.isBlank(pipeSeparatedConceptNames))
            return concepts;
        String[] fullyQualifiedConceptNames = pipeSeparatedConceptNames.split("\\s*\\|\\s*");
        return Arrays.stream(fullyQualifiedConceptNames).map(conceptService::getConcept).collect(Collectors.toList());
    }

    List<Concept> getOutcomeConcepts(){
        return getConcepts(CONCEPTS_GLOBAL_PROPERTY);
    }

    List<Concept> getProgramStateConcepts(){
        return getConcepts(PROGRAM_STATE_GLOBAL_PROPERTY);
    }

}
