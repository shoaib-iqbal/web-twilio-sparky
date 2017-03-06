package com.coresystems.sparky;

import com.coresystems.sparky.store.MockStore;
import com.coresystems.sparky.store.Registration;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.TestCase.*;


public final class MockStoreTest {

    @Test
    public void get() {
        MockStore store = new MockStore();
        assertNotNull(store.get(""));
        final String key = UUID.randomUUID().toString();
        final Registration registration = createRegistration();
        store.put(key, registration);
        assertNotNull(store.get(key));
        assertEquals(registration, store.get(key));
    }

    @Test
    public void store() {
        MockStore store = new MockStore();
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        final String key = UUID.randomUUID().toString();
        store.put(key, createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());

        assertEquals(4, store.getKeys().size());
        assertTrue(store.getKeys().contains(key));
    }

    @Test
    public void storeEmpty() {
        MockStore store = new MockStore();
        assertEquals(0, store.getKeys().size());
    }

    @Test
    public void storeAndCleanup() {
        final int maxCapacity = 2;
        MockStore store = new MockStore(maxCapacity);
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        assertEquals(maxCapacity, store.getKeys().size());
        //Max capacity is 2, so no matter how many entries we add after this, old ones should be removed
        final Registration value = createRegistration();
        final String key = UUID.randomUUID().toString();
        //Since we're having an order-based map, it should work in a FIFO fashion
        store.put(key, value);
        assertEquals(maxCapacity, store.getKeys().size());
        assertTrue(store.containsKey(key));
        //We'll add another one and our third entry should still be there
        store.put(UUID.randomUUID().toString(), createRegistration());
        assertEquals(maxCapacity, store.getKeys().size());
        assertTrue(store.containsKey(key));
        //Now we'll add another value, leading to the removal of the key we previously added
        store.put(UUID.randomUUID().toString(), createRegistration());
        assertEquals(maxCapacity, store.getKeys().size());
        assertFalse(store.containsKey(key));
    }

    @Test
    public void getKeyContains() {
        final String account = "SomeAccount";
        MockStore store = new MockStore();
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString() + account, createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString() + account, createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        assertEquals(6, store.getKeys().size());
        assertEquals(2, store.getKeyContains(store, account).size());
    }

    @Test
    public void containsKey() {
        MockStore store = new MockStore();
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());
        store.put(UUID.randomUUID().toString(), createRegistration());

        store.getKeys().forEach((key) -> assertTrue(store.containsKey(key)));

        for (String key : store.getKeys()) {
            assertTrue(store.containsKey(key));
        }
    }

    @Test
    public void isValidKey() {
        MockStore store = new MockStore();
        final Registration value = createRegistration();
        final String key = MockStore.Companion.createKey(value.getAccount(), value.getUserName());
        final Registration invalidValue = createRegistration();
        store.put(key, value);
        store.put(UUID.randomUUID().toString(), createRegistration());
        //The key/value has been inserted and should thus be present
        assertTrue(store.isValidEntry(value));
        //This hasn't been added to the store so we shouldn't find it
        assertFalse(store.isValidEntry(invalidValue));
        //Here the username + account matches but the key dinna match
        final Registration mixedValue = new Registration(value.getAccount(), value.getUserName(), value.getFullName(), UUID.randomUUID().toString(), key);
        assertFalse(store.isValidEntry(mixedValue));
    }

    @Test
    public void createKey() {
        assertEquals("AB", MockStore.Companion.createKey("A", "B"));
        assertEquals("B", MockStore.Companion.createKey("", "B"));
        assertEquals("B", MockStore.Companion.createKey("B", ""));
    }

    private static Registration createRegistration() {
        final String account = UUID.randomUUID().toString();
        final String userName = UUID.randomUUID().toString();
        return new Registration(account, userName, UUID.randomUUID().toString(), UUID.randomUUID().toString(), MockStore.Companion.createKey(account, userName));
    }
}
