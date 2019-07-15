package org.msf.module.visit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ModuleActivator;

public class CustomVisitCloseModuleActivator implements ModuleActivator {

    private Log log = LogFactory.getLog(getClass());

    /**
     * @see ModuleActivator#willRefreshContext()
     */
    public void willRefreshContext() {
        log.info("Refreshing visit-close-module");
    }

    /**
     * @see ModuleActivator#contextRefreshed()
     */
    public void contextRefreshed() {
        log.info("visit-close-module refreshed");
    }

    /**
     * @see ModuleActivator#willStart()
     */
    public void willStart() {
        log.info("Starting visit-close-module ");
    }

    /**
     * @see ModuleActivator#started()
     */
    public void started() {
        log.info("visit-close-module started");
    }

    /**
     * @see ModuleActivator#willStop()
     */
    public void willStop() {
        log.info("Stopping visit-close-module ");
    }

    /**
     * @see ModuleActivator#stopped()
     */
    public void stopped() {
        log.info("visit-close-module stopped");
    }
}
