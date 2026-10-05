package com.msgvault.app.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val THREAD = "thread/{address}/{contactName}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"

    fun thread(address: String, contactName: String): String {
        return "thread/${java.net.URLEncoder.encode(address, "UTF-8")}/${java.net.URLEncoder.encode(contactName, "UTF-8")}"
    }
}
