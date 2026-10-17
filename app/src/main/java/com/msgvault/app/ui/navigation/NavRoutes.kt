package com.msgvault.app.ui.navigation

object NavRoutes {
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val THREAD = "thread/{address}/{contactName}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
    const val ADMIN_CONVERSATIONS = "admin/conversations/{uid}/{displayName}"
    const val ADMIN_MESSAGES = "admin/messages/{uid}/{address}/{contactName}"

    fun thread(address: String, contactName: String): String {
        return "thread/${java.net.URLEncoder.encode(address, "UTF-8")}/${java.net.URLEncoder.encode(contactName, "UTF-8")}"
    }

    fun adminConversations(uid: String, displayName: String): String {
        return "admin/conversations/${java.net.URLEncoder.encode(uid, "UTF-8")}/${java.net.URLEncoder.encode(displayName, "UTF-8")}"
    }

    fun adminMessages(uid: String, address: String, contactName: String): String {
        return "admin/messages/${java.net.URLEncoder.encode(uid, "UTF-8")}/${java.net.URLEncoder.encode(address, "UTF-8")}/${java.net.URLEncoder.encode(contactName, "UTF-8")}"
    }
}
