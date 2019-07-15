package org.bahmni.module.visit;

import org.openmrs.api.AdministrationService;

import static org.openmrs.api.context.Context.getAdministrationService;

class GlobalPropertyReader {

    private AdministrationService administrationService;

    GlobalPropertyReader() {
        this.administrationService = getAdministrationService();
    }

    String getValueOfProperty(String property) {
        return administrationService.getGlobalPropertyValue(property, "");
    }
}
