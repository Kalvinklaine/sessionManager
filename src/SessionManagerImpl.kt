import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionManagerImpl() : SessionManager {
    private val sessions = mutableMapOf<String, Session>()
    private val sessionsByLastAccess = TreeMap<Long, MutableSet<String>>()
    private val sessionsByUser = mutableMapOf<String, MutableSet<String>>()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        startAutoExpire()
    }

    fun startAutoExpire() {
        executor.scheduleAtFixedRate({
            try {
                expire(System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 1, 10, TimeUnit.SECONDS)
    }

    override fun createSession(sessionId: String, userId: String, durationSeconds: Int, currentTime: Long) {
        if (sessions.containsKey(sessionId))
            throw IllegalStateException("Session already exists")

        sessions[sessionId] = Session(sessionId, userId, durationSeconds, lastAccesses = currentTime)
        sessionsByLastAccess.getOrPut(currentTime + durationSeconds * 1000) { mutableSetOf() }.add(sessionId)
        sessionsByUser.getOrPut(userId) { mutableSetOf() }.add(sessionId)
    }

    override fun touch(sessionId: String, currentTime: Long) {
        // Create session?
        val session = sessions[sessionId] ?: throw IllegalStateException("Session doesn't exist")

        sessionsByLastAccess[session.lastAccesses + session.durationSeconds * 1000]?.remove(sessionId)
        session.lastAccesses = currentTime
        sessionsByLastAccess.getOrPut(currentTime + session.durationSeconds * 1000) { mutableSetOf() }.add(sessionId)
    }

    override fun isActive(sessionId: String, currentTime: Long): Boolean {
        val session = sessions[sessionId] ?: return false
        return (currentTime - session.lastAccesses) < session.durationSeconds * 1000
    }

    override fun expire(currentTime: Long) {
        sessions.entries.removeIf { currentTime - it.value.lastAccesses > it.value.durationSeconds * 1000 }
        val expiredSessions = sessionsByLastAccess.headMap(currentTime, false)
        expiredSessions.forEach { (_, sessionIds) ->
            sessionIds.forEach {
                val userId = sessions[it]?.userId
                sessions.remove(it)
                if (userId != null) sessionsByUser[userId]?.remove(it)
            }
        }
        expiredSessions.clear()
    }

    override fun getSessionsByUser(userId: String): List<String> {
        sessionsByUser[userId]?.let {
            return it.toList()
        }
        return emptyList()
    }

    override fun getActiveSessionsByUser(userId: String, currentTime: Long): List<String> {
        sessionsByUser[userId]?.let { sessionIds ->
            return sessionIds.filter { sessionId ->
                val session = sessions[sessionId]
                session?.let {
                    (it.lastAccesses + it.durationSeconds * 1000) > currentTime
                } ?: false
            }
        }
        return emptyList()
    }
}

data class Session(val sessionId: String, val userId: String, val durationSeconds: Int, var lastAccesses: Long)