/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.refsetservice.rest.client;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.refsetservice.model.RestException;
import org.ihtsdo.refsetservice.model.User;
import org.ihtsdo.refsetservice.service.SecurityService;
import org.ihtsdo.refsetservice.util.CrowdGroupNameAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration to Atlassian's Crowd API.
 *
 */
public class CrowdAPIClient extends CrowdClientAbstract {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(CrowdAPIClient.class);

    /** Group name prefix for RT2 application. */
    private static final String APP_PREFIX = "rt2-";

    // USER
    /** Get user GET. */
    private static final String GET_USER = "/rest/usermanagement/1/user";

    /** Find user by email GET. */
    private static final String FIND_USER = "/rest/usermanagement/1/search?entity-type=user&restriction=email=";

    /** Get avatar for user EXPERIMENTAL GET. */
    private static final String GET_AVATAR_FOR_USER = "/rest/usermanagement/1/user/avatar?username=";

    /** Get direct groups GET. */
    private static final String GET_DIRECT_GROUPS = "/rest/usermanagement/1/user/group/direct?username=";

    // GROUP
    /** Get group memberships GET. */
    private static final String GET_MEMBERSHIPS = "/rest/usermanagement/1/group/membership";

    /** Add group POST. */
    private static final String ADD_GROUP = "/rest/usermanagement/1/group";

    // MEMBERSHIP
    /** Add a user to a group. */
    private static final String ADD_USER_TO_GROUP = "/rest/usermanagement/1/group/user/direct?groupname=";

    /** Remove user from group DELETE. */
    private static final String REMOVE_USER_FROM_GROUP = "/rest/usermanagement/1/user/group/direct";

    /** The Constant VALID_RULE_PARTS. */
    private static final int VALID_RULE_PARTS = 5;

    /**
     * Returns the user from Crowd.
     *
     * @param userName the user name
     * @return the user
     * @throws Exception the exception
     */
    public static User getUser(final String userName) throws Exception {

        LOG.debug("Get information for user {}", userName);

        if (StringUtils.isEmpty(userName)) {
            throw new Exception("User name cannot be empty or null. Received username: " + userName);
        }

        final HttpClient httpClient = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(getBaseUrl() + GET_USER + "?username=" + userName)).GET()
            .header("Accept", MediaType.APPLICATION_JSON).header("Authorization", getBasicAuthHeader()).build();
        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        // 200 OK.
        // 404 the user could not be found.
        if (response.statusCode() == 200) {

            final String jsonString = response.body();
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode root = mapper.readTree(jsonString);

            final User user = new User();
            user.setName(root.get("display-name").asText());
            user.setEmail(root.get("email").asText());
            user.setUserName(userName);

            return user;

        } else if (response.statusCode() == 400) {
            throw new Exception("The user " + userName + " could not be found.");
        } else {
            throw new Exception("The user " + userName + " could not be found. Received HTTP " + response.statusCode() + " from the API server.");
        }

    }

    /**
     * Add all groups with roles e.g. rt2-no-abc-author. - rt2 is the application - no is the two letter code for the organization (country) - abc is the
     * acronym of the group name - author is the role (admin, author, reviewer and viewer are the others)
     *
     * @param organizationName the organization name
     * @param editionName the edition name
     * @param projectName the project name
     * @param projectDescription the project description
     * @param generateProjectName the generate project name
     * @param adminOnly to add the all-admin permission for organization administrators
     * @throws Exception the exception
     */
    public static void addGroup(final String organizationName, final String editionName, final String projectName, final String projectDescription,
        final boolean generateProjectName, final boolean adminOnly) throws Exception {

        LOG.info("Add group {} to organization {} with description of {}", projectName, organizationName, projectDescription);

        if (StringUtils.isBlank(organizationName)) {
            throw new Exception("Organization name cannot be empty or null. Received organization: " + organizationName);
        }

        if (StringUtils.isBlank(editionName)) {
            throw new Exception("Edition name cannot be empty or null. Received edition: " + editionName);
        }

        if (StringUtils.isEmpty(projectName)) {
            throw new Exception("Project name cannot be empty or null. Received project: " + projectName);
        }

        final String description = (!StringUtils.isEmpty(projectDescription)) ? projectDescription.trim() : projectName.trim();

        /*
         * {"name": "rt2-ownerofinternational-test-all-author", "description": "test crowd client", "type": "GROUP" }
         */
        final Set<String> rolesToAdd = new HashSet<>();
        if (adminOnly) {
            rolesToAdd.add("admin");
        } else {
            rolesToAdd.addAll(ROLES);
        }

        for (final String role : rolesToAdd) {

            final String groupName =
                generateProjectName ? CrowdGroupNameAlgorithm.generateCrowdGroupName(organizationName, editionName, projectName, role, false)
                    : CrowdGroupNameAlgorithm.buildCrowdGroupName(organizationName, editionName, projectName, role);

            LOG.info("CALL CROWD API url:" + getBaseUrl() + ADD_GROUP);
            final String entity = "{\"name\": \"" + groupName + "\", \"description\": \"" + description + "\", \"type\": \"GROUP\" }";

            LOG.info("CALL CROWD API payload: " + entity);
            final int statusCode = post(getBaseUrl() + ADD_GROUP, entity);

            // 201 Returned if the group is successfully created.
            // 400 Returned if the group already exists.
            // 403 Returned if the application is not allowed to create a new group.
            if (statusCode == 201) {

                // expected 201 status, error occurred.
                LOG.info("Added group {}.", groupName);

            } else if (statusCode == 400) {

                LOG.info("Group already exists {}.", groupName);

            } else if (statusCode == 403) {

                LOG.error("The group " + groupName + " could not be created. Not allowed.");
                throw new Exception("The group " + groupName + " could not be created. Not allowed.");

            } else {
                LOG.error("The group " + groupName + " could not be created. Received HTTP " + statusCode + " from the API server.");
                throw new Exception("The group " + groupName + " could not be created. Received HTTP " + statusCode + " from the API server.");
            }
        }
    }

    /**
     * Add admin group for an organization.
     *
     * @param organizationName the organizationName
     * @param description the description
     * @return the string
     * @throws Exception the exception
     */
    public static String addAdminGroup(final String organizationName, final String description) throws Exception {

        LOG.info("Add group {} to organization {} with description of {}", "all", organizationName, description);

        if (StringUtils.isBlank(organizationName)) {
            throw new Exception("Organization name cannot be empty or null. Received organization: " + organizationName);
        }

        /*
         * {"name": "rt2-test-all-admin", "description": "admin for organization", "type": "GROUP" }
         */
        final String groupName = CrowdGroupNameAlgorithm.generateCrowdGroupName(organizationName, "all", "all", "admin", true);

        LOG.info("CALL CROWD API url:" + getBaseUrl() + ADD_GROUP);
        final String entity = "{\"name\": \"" + groupName + "\", \"description\": \"" + description + "\", \"type\": \"GROUP\" }";

        LOG.info("CALL CROWD API payload: " + entity);
        final int statusCode = post(getBaseUrl() + ADD_GROUP, entity);

        // 201 Returned if the group is successfully created.
        // 400 Returned if the group already exists.
        // 403 Returned if the application is not allowed to create a new group.
        if (statusCode == 201) {

            // expected 201 status, error occurred.
            LOG.info("Added group {}", groupName);
            return groupName;

        }

        if (statusCode == 400) {

            // ignore 400 and continue?
            LOG.error("The group " + groupName + " already exists");
            // throw new Exception("The group " + groupName + " already exists");
            return groupName;
        }

        if (statusCode == 403) {

            LOG.error("The group " + groupName + " could not be created. Not allowed.");
            throw new Exception("The group " + groupName + " could not be created. Not allowed.");

        }

        LOG.error("The group " + groupName + " could not be created. Received HTTP " + statusCode + " from the API server.");
        throw new Exception("The group " + groupName + " could not be created. Received HTTP " + statusCode + " from the API server.");

    }

    /**
     * Get URL to a user's avatar.
     * 
     * @param username The user's username.
     * @return String - URL for user's avatar.
     * @throws Exception the exception.
     */
    public static String getUserAvatar(final String username) throws Exception {

        LOG.debug("Get avatar for username {}", username);
        if (StringUtils.isBlank(username)) {
            throw new Exception("User name cannot be empty or null. Received username: " + username);
        }
        final String url = getBaseUrl() + GET_AVATAR_FOR_USER + username;
        final HttpClient httpClient = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().header("Accept", MediaType.APPLICATION_JSON)
            .header("Authorization", getBasicAuthHeader()).build();

        LOG.debug("CROWD API GET Url: {}", url);

        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        // 303 - The uri for the user's avatar (in the location header)
        // 404 - The user doesn't exist, or doesn't have an avatar defined
        if (response.statusCode() == 303) {

            LOG.debug("Found avatar for username {}", username);
            return response.headers().firstValue("location").orElse("");

        } else if (response.statusCode() == 404) {

            LOG.debug("Did not find avatar for username {}", username);
            return null;

        } else {

            LOG.debug("Did not find avatar for username {}", username);
            return "Did not find avatar for username " + username;

        }

    }

    /**
     * Returns the all groups.
     *
     * @return the all groups
     * @throws Exception the exception
     */
    public static Set<String> getAllGroups() throws Exception {

        LOG.debug("Get all groups with url: " + getBaseUrl() + GET_MEMBERSHIPS);

        final Set<String> userGroups = new HashSet<>();
        final String xmlString = get(getBaseUrl() + GET_MEMBERSHIPS, MediaType.APPLICATION_XML);

        try (final ByteArrayInputStream input = new ByteArrayInputStream(xmlString.toString().getBytes("UTF-8"));) {

            // Load the input XML document, parse it and return an instance of the
            // Document class.
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();

            final Document document = builder.parse(input);

            final NodeList groupList = document.getDocumentElement().getChildNodes();
            final int groupListSize = groupList.getLength();

            for (int i = 0; i < groupListSize; i++) {

                final Node groupNode = groupList.item(i);

                if (groupNode.getNodeType() == Node.ELEMENT_NODE) {

                    // Get the value of the group name attribute.
                    final String groupName = groupNode.getAttributes().getNamedItem("group").getNodeValue();
                    LOG.debug("groupName1: " + groupName);

                    if (groupName.startsWith(APP_PREFIX)) {
                        userGroups.add(groupName);
                    }

                } else {
                    LOG.error("groupNode Type2: " + groupNode.getNodeType());
                }
            }

            return userGroups;

        } catch (Exception e) {
            throw new Exception(
                "The groups could not be retrieved. Received HTTP " + xmlString + " from the API server with error Message--> " + e.getMessage());
        }
    }

    /**
     * Returns the all crowd rule members.
     * 
     * As rule-to-users map
     *
     * @return the all crowd rule members
     * @throws Exception the exception
     */
    public static Map<String, Set<String>> getAllCrowdRuleMembers() throws Exception {

        LOG.debug("Get all groups' members {}");

        final Map<String, Set<String>> groupMemberMap = new HashMap<>();
        LOG.debug("url: " + getBaseUrl() + GET_MEMBERSHIPS);

        final String xmlString = get(getBaseUrl() + GET_MEMBERSHIPS, MediaType.APPLICATION_XML);

        try (final ByteArrayInputStream input = new ByteArrayInputStream(xmlString.toString().getBytes("UTF-8"));) {

            // Load the input XML document, parse it and return an instance of the
            // Document class.
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();

            // Load the input XML document, parse it and return an instance of the
            // Document class.
            final Document document = builder.parse(input);

            final NodeList membershipList = document.getDocumentElement().getChildNodes();
            final int membershipListSize = membershipList.getLength();

            for (int i = 0; i < membershipListSize; i++) {

                final Node membershipNode = membershipList.item(i);

                if (membershipNode.getNodeType() == Node.ELEMENT_NODE && membershipNode.getNodeName().equals("membership")) {

                    final Element membership = (Element) membershipNode;
                    // Get the value of the group name attribute.
                    final String rule = membershipNode.getAttributes().getNamedItem("group").getNodeValue();

                    if (!rule.startsWith(APP_PREFIX) || !membership.hasChildNodes() || rule.split("-").length != VALID_RULE_PARTS) {
                        continue;
                    }

                    if (!groupMemberMap.containsKey(rule)) {
                        groupMemberMap.put(rule, new HashSet<>());
                    }

                    final NodeList usersList = membership.getChildNodes();
                    final int usersListSize = usersList.getLength();

                    for (int j = 0; j < usersListSize; j++) {
                        final Node usersNode = usersList.item(j);

                        if (usersNode.getNodeType() == Node.ELEMENT_NODE && usersNode.getNodeName().equals("users")) {

                            final Element users = (Element) usersNode;
                            final NodeList userList = users.getChildNodes();
                            final int userListSize = userList.getLength();

                            for (int k = 0; k < userListSize; k++) {

                                final Node userNode = userList.item(k);

                                if (usersNode.getNodeType() == Node.ELEMENT_NODE && userNode.getNodeName().equals("user")) {
                                    // Get the user name
                                    String userName = userNode.getAttributes().getNamedItem("name").getNodeValue();

                                    groupMemberMap.get(rule).add(userName);

                                }
                            }
                        }
                    }

                }
            }

            return groupMemberMap;

        } catch (Exception e) {
            throw new Exception(
                "The groups could not be retrieved. Received HTTP " + xmlString + " from the API server with error Message--> " + e.getMessage());
        }

    }

    /**
     * Calls a Crowd URL and returns the response with non-default MediaType (ACCEPT_DEFAULT) needed.
     *
     * @param url The Crowd URL to call
     * @param mediaType the media type
     * @return the response
     * @throws Exception the exception
     */
    private static String get(final String url, final String mediaType) throws Exception {

        final HttpClient httpClient = HttpClient.newBuilder().build();
        final HttpRequest request =
            HttpRequest.newBuilder().uri(URI.create(url)).GET().header("Authorization", getBasicAuthHeader()).header("Accept", mediaType).build();

        LOG.debug("CROWD API GET Url: {}", url);

        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {

            return response.body();

        }

        LOG.error("CROWD GET ERROR url: {} : response code: {}", url, response.statusCode());
        throw new Exception("CROWD GET ERROR url: " + url);
    }

    /**
     * Get list of user's group memberships.
     *
     * @param username the username
     * @return Set<String> List of user's groups.
     * @throws Exception the exception.
     */
    public static Set<String> getMembershipsForUser(final String username) throws Exception {

        LOG.debug("Get memberships for user {}", username);
        if (StringUtils.isBlank(username)) {
            throw new Exception("User name cannot be empty or null. Received username: " + username);
        }

        final Set<String> userGroups = new HashSet<>();
        final String jsonString = get(getBaseUrl() + GET_DIRECT_GROUPS + username);

        // 200 OK.
        // 404 the user could not be found or the user is not a direct member of the
        // specified group.

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(jsonString);
        final JsonNode groups = root.get("groups");
        if (groups != null && !groups.isEmpty()) {
            groups.forEach(groupName -> {
                final String name = groupName.findValue("name").asText();
                if (name.startsWith(APP_PREFIX)) {
                    userGroups.add(name);
                }
            });
        }
        return userGroups;
    }

    /**
     * Add a user to a group.
     * 
     * @param groupname Name of the group from which the user membership will be added.
     * @param username Name of the user to have their membership added.
     * @throws Exception the exception.
     */
    public static void addMembership(final String groupname, final String username) throws Exception {

        LOG.info("Add user {} to group {}", username, groupname);
        if (StringUtils.isBlank(groupname)) {
            throw new Exception("Group name cannot be empty or null. Received groupname: " + groupname);
        }
        if (StringUtils.isBlank(username)) {
            throw new Exception("User name cannot be empty or null. Received username: " + username);
        }

        final String body = "{ \"name\":\"" + username.trim() + "\" }";
        final int statusCode = post(getBaseUrl() + ADD_USER_TO_GROUP + groupname.trim(), body);

        // 201 Returned if the user is successfully added as a member of the group.
        // 400 Returned if the user could not be found or groupName is not specified or
        // user has no name.
        // 404 Returned if the group could not be found.
        // 409 Returned if the user is already a direct member of the group.
        if (statusCode == 201) {

            LOG.info("User {} now is a member of {}.", username, groupname);

            final User loggedInUser = SecurityService.getUserFromSession();

            // if the user modified is the current user then update their roles
            if (username.equals(loggedInUser.getUserName())) {

                loggedInUser.getRoles().add(groupname.substring("rt2-".length()));
                SecurityService.setUserInSession(loggedInUser);
            }

        } else if (statusCode == 400) {

            throw new Exception("Failed to add " + username.trim() + " to group " + groupname.trim() + ". "
                + "User could not be found or groupName is not specified or user has no name.");

        } else if (statusCode == 404) {

            throw new Exception("Failed to add username " + username.trim() + " to group " + groupname.trim() + ". Group could not be found.");

        } else if (statusCode == 409) {

            // throw new Exception("Failed to add username " + username.trim() + " to group
            // " + groupname.trim() + ". User is already a direct member of the group.");
            LOG.warn("Failed to add username " + username.trim() + " to group " + groupname.trim() + ". User is already a direct member of the group.");

        } else {
            throw new Exception(
                "Failed to add username " + username.trim() + " to group " + groupname.trim() + ". Received HTTP " + statusCode + " from the API server.");
        }
    }

    /**
     * Remove user's membership from a group.
     * 
     * @param groupname Name of the group from which the user membership will be removed.
     * @param username Name of the user to have their membership removed.
     * @throws Exception the exception
     */
    public static void deleteMembership(final String groupname, final String username) throws Exception {

        LOG.info("Remove user {} from group {}", username, groupname);
        if (StringUtils.isBlank(groupname)) {
            throw new Exception("Group name cannot be empty or null. Received groupname: " + groupname);
        }
        if (StringUtils.isBlank(username)) {
            throw new Exception("User name cannot be empty or null. Received username: " + username);
        }
        final int statusCode = delete(getBaseUrl() + REMOVE_USER_FROM_GROUP + "?groupname=" + groupname.trim() + "&username=" + username.trim());

        // 204 Returned if the user membership is successfully deleted.
        // 404 Returned if the user or group could not be found.
        if (statusCode == 204) {

            LOG.info("User {} removed from group {}.", username, groupname);

            final User loggedInUser = SecurityService.getUserFromSession();

            // if the user modified is the current user then update their roles
            if (username.equals(loggedInUser.getUserName())) {

                loggedInUser.getRoles().remove(groupname.substring("rt2-".length()));
                SecurityService.setUserInSession(loggedInUser);
            }

        } else if (statusCode == 404) {

            // throw new Exception("Failed to remove username " + username.trim() + " from
            // group " + groupname.trim() + ". Group could not be found.");
            LOG.info("Failed to remove username " + username.trim() + " from group " + groupname.trim() + ". Group could not be found.");

        } else {

            throw new Exception(
                "Failed to remove username " + username.trim() + " from group " + groupname.trim() + ". Received HTTP " + statusCode + " from the API server.");

        }
    }

    /**
     * Find user by email.
     *
     * @param email the email
     * @return the user
     * @throws Exception the exception
     */
    public static User findUserByEmail(final String email) throws Exception {

        LOG.info("Find user by email: {} ", email);

        if (StringUtils.isBlank(email)) {
            LOG.warn("Email is blank or empty. Received email: " + email);
            return null;
        }

        final String jsonString = get(getBaseUrl() + FIND_USER + urlEncode(email));
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(jsonString);
        final JsonNode users = root.get("users");

        if (users == null || (users.isArray() && users.isEmpty())) {

            return null;
        }

        if (users.isArray() && users.size() > 1) {
            throw new RestException(false, HttpStatus.CONFLICT, "Found multiple",
                "Found multiple users with email of " + email + ". Can't determine which user to create.");
        }

        final String name = users.get(0).findValue("name").asText();
        return (StringUtils.isNotBlank(name)) ? getUser(name) : null;

    }
}
