
package org.ihtsdo.refsetservice.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler.
 */
@Configuration
@EnableScheduling
public class Scheduler {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

    /**
     * Instantiates an empty {@link Scheduler}.
     */
    public Scheduler() {

        LOG.debug("Creating instance of class Scheduler");
    }

}
