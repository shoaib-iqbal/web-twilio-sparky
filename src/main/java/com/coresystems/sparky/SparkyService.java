package com.coresystems.sparky;

import com.coresystems.sparky.store.KeyValueStorage;
import com.coresystems.sparky.store.MockStore;
import com.coresystems.sparky.store.Registration;
import com.google.gson.reflect.TypeToken;
import com.twilio.sdk.auth.AccessToken;
import com.twilio.sdk.auth.ConversationsGrant;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class that processes external requests.
 */
public final class SparkyService {
    private static final String DEFAULT_USERNAME = "Patrick";
    private static final String DEFAULT_ACCOUNT = "core-plda-et";
    private static final String DEFAULT_FULL_NAME = String.format("%s %s", DEFAULT_USERNAME, "Jane");

    /**
     * The following variables are environment variables, so make sure you set them with your account credentials
     **/
    // Your primary Twilio account identifier - check the README for details
    static final String ACCOUNT_SID = "ACCOUNT_SID";
    // The Twilio Video Configuration SID that you generated when you opted into the programmable video beta feature - check the README for details
    static final String VIDEO_CONFIGURATION_SID = "VIDEO_CONFIGURATION_SID";
    // API Key obtained via your Twilio account - check the README for details
    static final String API_KEY = "API_KEY";
    // API Secret obtained via your Twilio account - check the README for details
    static final String API_SECRET = "API_SECRET";

    // Other identifiers
    private static final String USERNAME = "userName";
    private static final String ACCOUNT = "account";
    private static final String FULL_NAME = "fullName";
    private static final String TOKEN = "token";
    private static final String CALLER_ID = "callerId";
    private static final String FORCE_REAUTHENTICATION = "forceReAuthentication";

    //In the twilio SDK the TTL is multiplied by 1000 - we need to keep this in mind when setting our own TTL (12 hours)
    private static final int TOKEN_TTL = 43200;

    //Empty default values
    private static final List<Registration> EMPTY_USER_LIST = new ArrayList<>();
    private static final Registration EMPTY_REGISTRATION = new Registration();

    private final KeyValueStorage<String, Registration> store;

    /**
     * Creates a new instance of this SparkyService with the given store.
     *
     * @param store the store used by this service.
     */
    SparkyService(KeyValueStorage<String, Registration> store) {
        this.store = store;
    }

    /**
     * We read the environment variables each time anew so the server does not require a restart whenever those change.
     *
     * @return Properties read on-the-fly from the set environment variables.
     */
    @NotNull
    static Properties getStaticProperties() {
        Properties properties = new Properties();
        // The following credentials are from the twilio account you're using, they must be set as environment variables
        properties.put(ACCOUNT_SID, System.getenv(ACCOUNT_SID));
        properties.put(VIDEO_CONFIGURATION_SID, System.getenv(VIDEO_CONFIGURATION_SID));
        properties.put(API_KEY, System.getenv(API_KEY));
        properties.put(API_SECRET, System.getenv(API_SECRET));
        return properties;
    }

    /**
     * @param properties  environment properties.
     * @param jsonRequest the get request from the client seeking authentication.
     * @return a json response String containing a Registration instance.
     */
    @NotNull
    String getAuthenticationJson(@NotNull Properties properties, @Nullable String jsonRequest) {
        /*
        TODO once the web interface is able to authenticate itself with differing, non-static credentials
        we should fail if either account, username or fullname are empty
         */
        final Map<String, String> json = replaceEmptyWithDefault(jsonToMap(jsonRequest));
        Webapp.logger.info("Received authentication request by: " + jsonRequest);

        final String key = MockStore.Companion.createKey(json.get(ACCOUNT), json.get(USERNAME));
        final String jwtToken;
        if (store.containsKey(key) && !Boolean.valueOf(json.get(FORCE_REAUTHENTICATION))) {
            //Check if we already have a token for the given account/user
            final Registration registration = store.get(key);
            jwtToken = registration.getToken();
        } else {
            // Create Conversations messaging grant
            ConversationsGrant grant = new ConversationsGrant();
            grant.configurationProfileSid = properties.getProperty(VIDEO_CONFIGURATION_SID);
            // Create access token - we use the "key" to create the identity, the client will be given that key to call another user
            AccessToken token = new AccessToken.Builder(properties.getProperty(ACCOUNT_SID), properties.getProperty(API_KEY), properties.getProperty(API_SECRET))
                    .identity(key)
                    .grant(grant)
                    .ttl(TOKEN_TTL)
                    .build();
            jwtToken = token.toJWT();
            //Cache the user for later access
            store.put(key, new Registration(json.get(ACCOUNT), json.get(USERNAME), json.get(FULL_NAME), jwtToken, key));
        }
        json.put(TOKEN, jwtToken);
        json.put(CALLER_ID, key);

        // Render JSON response
        return Webapp.gson.toJson(json);
    }


    /**
     * @param jsonRequest the json containing details about the requester of the users.
     * @return an empty user list if the credentials were invalid or there are no registered users; all user's for the requesters account otherwise.
     */
    @NotNull
    List<Registration> getAuthenticatedUsersByAccount(@Nullable String jsonRequest) {
        final Registration registration = fromJson(jsonRequest);
        Webapp.logger.info("Received request to obtain user list by: " + registration.getFullName());

        List<Registration> result;
        if (store.isValidEntry(new Registration(registration.getAccount(), registration.getUserName(), StringUtils.EMPTY,
                registration.getToken(), StringUtils.EMPTY))) {
            result = store.getKeyContains(store, registration.getAccount());
            //we don't want to pass on the user's token to anyone else but that particular user
            //We'll also filter out the original requester
            result = result.stream()
                    .filter(predicate -> !predicate.getUserName().equals(registration.getUserName()))
                    .map(entry -> new Registration(entry.getAccount(), entry.getUserName(), entry.getFullName(),
                            StringUtils.EMPTY, MockStore.Companion.createKey(entry.getAccount(), entry.getUserName())))
                    .collect(Collectors.toList());
        } else {
            Webapp.logger.info("The request was invalid due to missing information or the user was not registered");
            result = EMPTY_USER_LIST;
        }
        Webapp.logger.info("Number of users registered for the given company: " + result.size());
        return result;
    }

    /**
     * @return the registration that was removed; an empty Registration otherwise.
     */
    @NotNull
    Registration removeRegistration(@Nullable String jsonRequest) {
        Registration registration = fromJson(jsonRequest);
        Webapp.logger.info("Received request to remove a registration by: " + registration.getFullName());
        final String key = MockStore.Companion.createKey(registration.getAccount(), registration.getUserName());
        if (store.isValidEntry(registration)) {
            store.remove(key);
        } else {
            registration = EMPTY_REGISTRATION;
        }
        return registration;
    }

    /**
     * @param json the json string.
     * @return an instance of {@link Registration} whose values are set based on the provided json.
     */
    @NotNull
    static Registration fromJson(@Nullable String json) {
        if (StringUtils.isEmpty(json)) {
            return EMPTY_REGISTRATION;
        } else {
            Map<String, String> map = jsonToMap(json);
            final String account = getStringValue(map.get(ACCOUNT), StringUtils.EMPTY);
            final String userName = getStringValue(map.get(USERNAME), StringUtils.EMPTY);
            final String fullName = getStringValue(map.get(FULL_NAME), StringUtils.EMPTY);
            final String token = getStringValue(map.get(TOKEN), StringUtils.EMPTY);
            final String key = getStringValue(map.get(CALLER_ID), StringUtils.EMPTY);
            return new Registration(account, userName, fullName, token, key);
        }
    }

    /**
     * Checks each entry in the given map and if empty replaces it with the appropriate default value.
     *
     * @param registrationValues the map with values to check and replace if empty/null.
     * @return a new map containing the updated values.
     */
    static Map<String, String> replaceEmptyWithDefault(@NotNull Map<String, String> registrationValues) {
        Map<String, String> result = new HashMap<>(registrationValues);
        if (StringUtils.isEmpty(result.get(ACCOUNT))) {
            result.put(ACCOUNT, DEFAULT_ACCOUNT);
        }
        if (StringUtils.isEmpty(result.get(USERNAME))) {
            result.put(USERNAME, DEFAULT_USERNAME);
        }
        if (StringUtils.isEmpty(result.get(FULL_NAME))) {
            result.put(FULL_NAME, DEFAULT_FULL_NAME);
        }
        if (result.containsKey(FORCE_REAUTHENTICATION)) {
            result.put(FORCE_REAUTHENTICATION, String.valueOf(result.get(FORCE_REAUTHENTICATION)));
        } else {
            result.put(FORCE_REAUTHENTICATION, String.valueOf(false));
        }
        result.putIfAbsent(TOKEN, StringUtils.EMPTY);

        return result;
    }


    /**
     * @param value        the value to return if not null or empty.
     * @param defaultValue the default value that is returned if the given value is null or empty.
     * @return the given value if it is not null or empty; the defaultValue otherwise.
     */
    @NotNull
    static String getStringValue(String value, @NotNull String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }


    /**
     * @param json the json string.
     * @return the given json is converted to a map. If the given json argument is null or empty, an empty map is returned.
     */
    @NotNull
    private static Map<String, String> jsonToMap(@Nullable String json) {
        if (!StringUtils.isEmpty(json)) {
            final Type stringStringMap = new TypeToken<Map<String, String>>() {
            }.getType();
            return Webapp.gson.fromJson(json, stringStringMap);
        }
        return new HashMap<>();
    }
}
