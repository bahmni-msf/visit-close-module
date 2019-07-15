package org.bahmni.module.visit;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.List;

class VisitClose {

    private static final String VISIT_TYPE_GLOBAL_PROPERTIEY = "visits.closeOnAnOutcome.visitType(s)";
    private GlobalPropertyReader globalPropertyReader;
    private VisitService visitService;

    VisitClose() {
        this.globalPropertyReader = new GlobalPropertyReader();
        this.visitService = Context.getVisitService();
    }

    List<VisitType> getVisitTypes() {
        ArrayList<VisitType> visitTypes = new ArrayList<>();
        String commaSeparatedVisitTypes = globalPropertyReader.getValueOfProperty(VISIT_TYPE_GLOBAL_PROPERTIEY);
        if (StringUtils.isBlank(commaSeparatedVisitTypes))
            return visitTypes;
        String[] visitTypeNames = commaSeparatedVisitTypes.split("\\s*,\\s*");
        for (String visitTypeName : visitTypeNames) {
            List<VisitType> matchedVisitTypes = visitService.getVisitTypes(visitTypeName);
            visitTypes.addAll(matchedVisitTypes);
        }
        return visitTypes;
    }

}
