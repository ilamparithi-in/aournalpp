package dev.ilamparithi.aournalpp.testutils

import android.content.SharedPreferences

class TestSharedPreferences(
    private val data: MutableMap<String, Any?> = mutableMapOf()
) : SharedPreferences, SharedPreferences.Editor {

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (data[key] as? Number)?.toInt() ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (data[key] as? Number)?.toLong() ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (data[key] as? Number)?.toFloat() ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = this

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        if (key != null) data[key] = value
        return this
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        if (key != null) data[key] = values?.toSet()
        return this
    }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        if (key != null) data[key] = value
        return this
    }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        if (key != null) data[key] = value
        return this
    }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        if (key != null) data[key] = value
        return this
    }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        if (key != null) data[key] = value
        return this
    }

    override fun remove(key: String?): SharedPreferences.Editor {
        if (key != null) data.remove(key)
        return this
    }

    override fun clear(): SharedPreferences.Editor {
        data.clear()
        return this
    }

    override fun commit(): Boolean = true

    override fun apply() {}
}
