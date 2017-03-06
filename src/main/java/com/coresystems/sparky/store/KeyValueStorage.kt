package com.coresystems.sparky.store

/**
 * Abstraction layer for Key-Value store (most likely it'll be an amazon S3 storage).
 */
interface KeyValueStorage<K, V> {
    fun put(key: K, value: V)

    fun get(key: K): V

    fun remove(key: K)

    fun containsKey(key: K): Boolean

    fun getKeys(): Collection<K>

    fun isValidEntry(value: V): Boolean

    /**
     * Returns a list of strings based on the V value. The list contains all values mapped to keys which contain (not equal - but contain) the given filter.
     */
    fun getKeyContains(store: KeyValueStorage<K, V>, filter: String): List<V>
}
