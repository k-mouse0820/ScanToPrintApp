package jp.daisen_solution.scantoprintapp

import java.util.concurrent.atomic.AtomicBoolean

class ConnectionData {

    var issueMode = 0
    var portSetting: String? = null
    var isOpen = AtomicBoolean(false)

}