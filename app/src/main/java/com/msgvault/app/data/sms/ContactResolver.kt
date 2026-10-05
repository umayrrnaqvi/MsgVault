package com.msgvault.app.data.sms

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = mutableMapOf<String, String?>()

    fun resolve(phoneNumber: String): String? {
        cache[phoneNumber]?.let { return it }

        val name = lookupContact(phoneNumber)
        cache[phoneNumber] = name
        return name
    }

    fun clearCache() {
        cache.clear()
    }

    private fun lookupContact(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
