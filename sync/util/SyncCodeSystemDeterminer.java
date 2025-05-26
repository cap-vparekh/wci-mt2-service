/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.refsetservice.sync.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.ihtsdo.refsetservice.service.TerminologyService;
import org.ihtsdo.refsetservice.sync.SyncCodeSystemAgent;
import org.ihtsdo.refsetservice.terminologyservice.SnowstormConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class SyncCodeSystemDeterminer.
 */
public class SyncCodeSystemDeterminer {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(SyncCodeSystemAgent.class);

    /** The Constant MANAGED_SERVICE_CONTAINER_TYPE. */
    private static final String MANAGED_SERVICE_CONTAINER_TYPE = "Managed Service";

    /** The Constant AFFILIATE_OWNER. */
    private static final String AFFILIATE_OWNER = "Affiliates";

    /** The service. */
    private TerminologyService service;

    /** The sync utilities. */
    private SyncUtilities syncUtilities;

    /** The statistics. */
    private SyncStatistics statistics;

    /** The is testing. */
    private boolean isTesting;

    /** The testing edition short name. */
    private String testingEditionShortName;

    /**
     * Instantiates a {@link SyncCodeSystemDeterminer} from the specified parameters.
     *
     * @param service the service
     * @param syncUtilities the sync utilities
     * @param syncStatistics the sync statistics
     * @param isTesting the is testing
     * @param testingEditionShortName the testing edition short name
     */
    public SyncCodeSystemDeterminer(final TerminologyService service, final SyncUtilities syncUtilities, final SyncStatistics syncStatistics,
        final boolean isTesting, final String testingEditionShortName) {

        this.service = service;
        this.syncUtilities = syncUtilities;
        this.statistics = syncStatistics;
        this.isTesting = isTesting;
        this.testingEditionShortName = testingEditionShortName;
    }

    /**
     * Determine code systems to process.
     *
     * @return the sets the
     * @throws Exception the exception
     */
    public Set<JsonNode> determineCodeSystemsToProcess() throws Exception {

        final Set<JsonNode> filteredCodeSystems = new HashSet<>();

        // Get all code systems from Snowstorm
        final JsonNode organizationJsonRootNode = getSnowstormCodeSystems();
        LOG.info("Found " + countCodeSystems(organizationJsonRootNode) + " + Code Systems on term server ");

        // Filter code systems (based on active-setting, ignoredCS list, testing situation, and bad data)
        final Map<SyncReasonEditionSkipped, Set<String>> ignoredReasonsEditionMap =
            identifyEditionsToSkip(service, organizationJsonRootNode, syncUtilities, isTesting);

        filteredCodeSystems.addAll(filterValidCodeSystems(service, organizationJsonRootNode, ignoredReasonsEditionMap, syncUtilities));

        LOG.info("Will be processing these " + filteredCodeSystems.size() + " Code Systems found on the term server: ");
        filteredCodeSystems.stream().forEach(c -> LOG.info(c.get("shortName").asText()));

        // Stats
        statistics.setCodeSystemsSynced(countCodeSystems(organizationJsonRootNode));
        statistics.setCodeSystemsFiltered(filteredCodeSystems.size());

        return filteredCodeSystems;
    }

    /**
     * Returns the edition to organization map.
     *
     * @param filteredCodeSystems the filtered code systems
     * @return the edition to organization map
     */
    public HashMap<String, String> getEditionToOrganizationMap(final Set<JsonNode> filteredCodeSystems) {

        final HashMap<String, String> termServerEditionToOrganizationMap = new HashMap<>();

        // Determine Snow edition-to-orgName map
        for (final JsonNode codeSystem : filteredCodeSystems) {

            final String shortName = codeSystem.get("shortName").asText();
            final String organizationName = syncUtilities.determineOrganizationName(codeSystem);

            termServerEditionToOrganizationMap.put(shortName, organizationName);
        }

        return termServerEditionToOrganizationMap;
    }

    /**
     * Identify editions to skip.
     *
     * @param service the service
     * @param organizationJsonRootNode the organization json root node
     * @param syncUtilities the sync utilities
     * @param isTesting the is testing
     * @return the map
     * @throws Exception the exception
     */
    private Map<SyncReasonEditionSkipped, Set<String>> identifyEditionsToSkip(final TerminologyService service, final JsonNode organizationJsonRootNode,
        final SyncUtilities syncUtilities, final boolean isTesting) throws Exception {

        final Iterator<JsonNode> organizationIterator = organizationJsonRootNode.iterator();
        final Map<SyncReasonEditionSkipped, Set<String>> ignoredReasonEditionMap = new EnumMap<>(SyncReasonEditionSkipped.class);
        SyncReasonEditionSkipped.getAllReasons().stream().forEach(reason -> ignoredReasonEditionMap.put(reason, new HashSet<>()));

        while (organizationIterator.hasNext()) {

            final Iterator<JsonNode> codeSystems = organizationIterator.next().iterator();

            while (codeSystems.hasNext()) {

                final JsonNode codeSystem = codeSystems.next();

                // Check for invalid or ignored code systems
                if (!codeSystem.has("shortName")) {

                    // Skipping odd code system without a shortName
                    LOG.error("Skipping codeSystem without a shortName: " + codeSystem);
                    continue;
                }

                final String editionShortName = codeSystem.get("shortName").asText();
                final String organizationName = codeSystem.has("owner") ? codeSystem.get("owner").asText() : "";

                final String maintainerType = syncUtilities.identifyMaintainerType(codeSystem, editionShortName);

                if (isTesting && !isTestingEditionToProcess(editionShortName, syncUtilities)) {

                    ignoredReasonEditionMap.get(SyncReasonEditionSkipped.WRONG_TESTING_EDITION).add(editionShortName);

                } else if (codeSystem.has("active") && !codeSystem.get("active").asBoolean()) {

                    // Skipping inactive code system
                    ignoredReasonEditionMap.get(SyncReasonEditionSkipped.INACTIVE_EDITION).add(editionShortName);

                } else if (!syncUtilities.isInternationalEdition(editionShortName) && !MANAGED_SERVICE_CONTAINER_TYPE.equals(maintainerType)) {

                    // Skipping inactive code system
                    ignoredReasonEditionMap.get(SyncReasonEditionSkipped.NON_SUPPORTED_MAINTAINER_TYPE).add(editionShortName);

                } else if (AFFILIATE_OWNER.equals(organizationName)) {

                    // Skipping affiliate code systems
                    ignoredReasonEditionMap.get(SyncReasonEditionSkipped.AFFILIATE_CODE_SYSTEM).add(editionShortName);

                } else if (syncUtilities.getPropertyReader().getCodeSystemsToIgnore().contains(editionShortName)) {

                    // Code System has been defined as to-be-ignored (either by specifying name or shortname).
                    ignoredReasonEditionMap.get(SyncReasonEditionSkipped.IGNORED_PER_FILE_EDITION).add(editionShortName);

                }

            }

        }

        // Log why each edition that isn't being processed is being skipped
        for (final SyncReasonEditionSkipped reason : ignoredReasonEditionMap.keySet()) {

            if (!ignoredReasonEditionMap.get(reason).isEmpty()) {

                final StringBuffer s = new StringBuffer("Ignoring these editions as they are: ");
                s.append(System.lineSeparator());

                switch (reason) {

                    case WRONG_TESTING_EDITION:
                        s.append("not the testing edition specified");
                        break;
                    case INACTIVE_EDITION:
                        s.append("inactive");
                        break;
                    case IGNORED_PER_FILE_EDITION:
                        // Ideally, we don't rely on this, but rather programmatically calculate based on code system attributes 
                        s.append("listed in ignoredCodeSystems.txt");
                        break;
                    case NON_SUPPORTED_MAINTAINER_TYPE:
                        s.append("not supported maintainer type");
                        break;
                    case AFFILIATE_CODE_SYSTEM:
                        s.append("affiliate code system");
                        break;
                    default:
                        break;
                }

                s.append(": ");
                ignoredReasonEditionMap.get(reason).stream().forEach(editionShortName -> s.append(editionShortName + ","));
                LOG.info(s.substring(0, s.toString().length()));
            }

        }

        return ignoredReasonEditionMap;
    }

    /**
     * Filter code systems.
     *
     * @param service the service
     * @param organizationJsonRootNode the organization json root node
     * @param ignoredReasonsEditionMap the ignored reasons edition map
     * @param syncUtilities the sync utilities
     * @return the map
     * @throws Exception the exception
     */
    private Set<JsonNode> filterValidCodeSystems(final TerminologyService service, final JsonNode organizationJsonRootNode,
        final Map<SyncReasonEditionSkipped, Set<String>> ignoredReasonsEditionMap, final SyncUtilities syncUtilities) throws Exception {

        final Set<JsonNode> filteredCodeSystems = new HashSet<>();

        // identify ignored edition short names
        final Set<String> ignoredEditions = new HashSet<>();
        ignoredReasonsEditionMap.values().stream()
            .forEach(editionList -> editionList.stream().forEach(editionShortName -> ignoredEditions.add(editionShortName)));

        final Iterator<JsonNode> organizationIterator = organizationJsonRootNode.iterator();

        while (organizationIterator.hasNext()) {

            final Iterator<JsonNode> codeSystems = organizationIterator.next().iterator();

            while (codeSystems.hasNext()) {

                final JsonNode codeSystem = codeSystems.next();

                // Check for invalid or ignored code systems
                if (!codeSystem.has("shortName")) {

                    // Skipping odd code system without a shortName
                    LOG.error("Skipping codeSystem without a shortName: " + codeSystem);
                    continue;
                }

                final String editionShortName = codeSystem.get("shortName").asText();

                // if ( editionShortName.equals("SNOMEDCT-NO") || editionShortName.equals("SNOMEDCT-SE") ) {

                if (!ignoredEditions.contains(editionShortName)) {

                    filteredCodeSystems.add(codeSystem);
                }

                // }

            }

        }

        return filteredCodeSystems;
    }

    /**
     * Count code systems.
     *
     * @param organizationJsonRootNode the organization iterator
     * @return the int
     */
    private int countCodeSystems(final JsonNode organizationJsonRootNode) {

        final Iterator<JsonNode> organizationIterator = organizationJsonRootNode.iterator();

        int counter = 0;

        while (organizationIterator.hasNext()) {

            final Iterator<JsonNode> codeSystems = organizationIterator.next().iterator();

            while (codeSystems.hasNext()) {

                counter++;
                codeSystems.next();
            }

        }

        return counter;
    }

    /**
     * Populate editions.
     *
     * @return the sets the
     * @throws Exception the exception
     */
    private JsonNode getSnowstormCodeSystems() throws Exception {

        final String url = SnowstormConnection.getBaseUrl() + "codesystems";
        LOG.info("getSnowstormCodeSystems url: " + url);

        try (final Response response = SnowstormConnection.getResponse(url)) {

            final String resultString = response.readEntity(String.class);

            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode organizationJsonRootNode = mapper.readTree(resultString.toString());

            return organizationJsonRootNode;
        }

    }

    /**
     * Indicates whether or not testing edition to process is the case.
     *
     * @param codeSystem the code system
     * @param syncUtilities the sync utilities
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean isTestingEditionToProcess(final String codeSystem, final SyncUtilities syncUtilities) {

        return ((testingEditionShortName == null || testingEditionShortName.isEmpty()) || codeSystem.equalsIgnoreCase(testingEditionShortName));

    }
}
