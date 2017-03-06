package com.coresystems.sparky.store

import java.util.*

/**
 * Stores mock data - will be replaced with the S3 eventually.
 */
class MockStore constructor(maxCapacity: Int = 1000) : KeyValueStorage<String, Registration> {

    companion object {
        /**
         * The key will later be used by the clients to send call requests to each other.
         * @param account  the account name.
         * @param userName the username belonging to the given account.
         * @return a key used to store a user's registration.
         */
        fun createKey(account: String, userName: String): String {
            return account + userName
        }
    }

    private val store: MutableMap<String, Registration> = Collections.synchronizedMap(StoreMap(maxCapacity / 2, maxCapacity))

    override fun isValidEntry(value: Registration): Boolean {
        //When validating an entry we care about: account, username and the access token - the full name dinna matter
        val key: String = createKey(value.account, value.userName)
        if (containsKey(key)) {
            val registration: Registration = store.get(createKey(value.account, value.userName))!!
            //We already know that the account & username match, since we found an entry for the key, so we'll just compare the token
            return registration.token == value.token
        }
        return false
    }

    override fun getKeyContains(store: KeyValueStorage<String, Registration>, filter: String): List<Registration> {
        return store.getKeys()
                .filter { it.contains(filter) }
                .map { store.get(it) }
    }

    override fun containsKey(key: String): Boolean = store.contains(key)

    override fun put(key: String, value: Registration) {
        store.put(key, value)
    }

    override fun get(key: String): Registration {
        if (store.containsKey(key)) {
            return store.get(key)!!
        }
        return Registration()
    }

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun getKeys(): Collection<String> = store.keys

    class StoreMap(initialCapacity: Int, val maxCapacity: Int) : LinkedHashMap<String, Registration>(initialCapacity) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Registration>?): Boolean = size > maxCapacity
    }
}