package no.nav.pto.veilarbportefolje.util


import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

object SecureLog {
    @JvmField
    val secureLog = TeamLogsWrapper()
}

class TeamLogsWrapper() {
    val marker = MarkerFactory.getMarker("TEAM_LOGS")
    val teamLog = LoggerFactory.getLogger("team-logs-logger")

    fun debug(msg: String, vararg args: Any) = teamLog.info(marker, msg, *args)

    fun info(msg: String, vararg args: Any) = teamLog.info(marker, msg, *args)

    @JvmOverloads
    fun warn(msg: String, error: Throwable? = null, vararg args: Any) {
        if (error != null) {
            teamLog.warn(marker, msg, error)
        } else {
            teamLog.warn(marker, msg, *args)
        }
    }

    @JvmOverloads
    fun error(msg: String, error: Throwable? = null, vararg args: Any) {
        if (error != null) {
            teamLog.error(marker, msg, error)
        } else {
            teamLog.error(marker, msg, *args)
        }
    }
}
