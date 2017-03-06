package com.coresystems.sparky;

import com.coresystems.sparky.store.MockStore;
import com.coresystems.sparky.store.Registration;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static com.coresystems.sparky.SparkyService.*;
import static junit.framework.TestCase.*;

public final class SparkyServiceTest {

    @Test
    public void getStringValue() {
        assertEquals("", SparkyService.getStringValue(null, ""));
        assertEquals("Hello", SparkyService.getStringValue("", "Hello"));
        assertEquals("Hello", SparkyService.getStringValue("Hello", "B"));
    }

    @Test
    public void fromJson() {
        assertNotNull(SparkyService.fromJson(null));
        assertNotNull(SparkyService.fromJson(""));

        Registration result = SparkyService.fromJson("{\"userName\":\"rjohn\",\"account\":\"CBI\",\"fullName\":\"Red John\",\"callerId\":\"CBIrjohn\"}");
        assertEquals("rjohn", result.getUserName());
        assertEquals("Red John", result.getFullName());
        assertEquals("CBI", result.getAccount());
        assertEquals("", result.getToken());
        assertEquals("CBIrjohn", result.getCallerId());
    }

    @Test
    public void getAuthenticationJson() {
        final Properties properties = getStaticProperties();
        //No matter how many time we execute below code, the result should always be the same
        for (int i = 0; i < 5; i++) {
            String jsonRequest = "{\"userName\":\"rjohn\",\"account\":\"CBI\",\"fullName\":\"Red John\"}";
            SparkyService service = new SparkyService(new MockStore(5));
            String jsonResponse = service.getAuthenticationJson(properties, jsonRequest);
            assertNotNull(jsonResponse);
            String errorMessage = "Failed on execution count " + i;
            assertTrue(errorMessage, jsonResponse.contains("\"userName\":\"rjohn\""));
            assertTrue(errorMessage, jsonResponse.contains("\"account\":\"CBI\""));
            assertTrue(errorMessage, jsonResponse.contains("\"callerId\":\"CBIrjohn\""));
            assertTrue(errorMessage, jsonResponse.contains("\"fullName\":\"Red John\""));
        }
    }

    /**
     * @return dummy test properties.
     */
    @NotNull
    private static Properties getStaticProperties() {
        Properties properties = new Properties();
        // The following credentials are from the twilio account you're using, they must be set as environment variables
        properties.put(ACCOUNT_SID, UUID.randomUUID().toString());
        properties.put(VIDEO_CONFIGURATION_SID, UUID.randomUUID().toString());
        properties.put(API_KEY, UUID.randomUUID().toString());
        properties.put(API_SECRET, UUID.randomUUID().toString());
        return properties;
    }

    @Test
    public void replaceEmptyWithDefaultNonEmptyMap() {
        Map<String, String> map = new HashMap<>();
        //Change all values in the map and run the replace again
        map.put("userName", "rjohn");
        map.put("fullName", "Red John");
        final String account = UUID.randomUUID().toString();
        map.put("account", account);
        map.put("forceReAuthentication", String.valueOf(true));

        //Nothing should have been replaced, since all values (except token) were filled
        map = SparkyService.replaceEmptyWithDefault(map);
        assertEquals("rjohn", map.get("userName"));
        assertEquals("Red John", map.get("fullName"));
        assertEquals(account, map.get("account"));
        assertEquals(StringUtils.EMPTY, map.get("token"));
        assertEquals("true", map.get("forceReAuthentication"));
    }

    @Test
    public void replaceEmptyWithDefaultEmptyMap() {
        //We start out with an empty map, so we expect all values to be replaced with the defaults
        Map<String, String> map = SparkyService.replaceEmptyWithDefault(new HashMap<>());
        assertEquals("Patrick", map.get("userName"));
        assertEquals("Patrick Jane", map.get("fullName"));
        assertEquals("core-plda-et", map.get("account"));
        assertEquals(StringUtils.EMPTY, map.get("token"));
        assertEquals("false", map.get("forceReAuthentication"));
    }
}