
interface SessionManager {
    fun createSession(sessionId: String, userId: String, durationSeconds: Int, currentTime: Long)
    fun touch(sessionId: String, currentTime: Long)
    fun isActive(sessionId: String, currentTime: Long): Boolean
    fun expire(currentTime: Long)
    fun getSessionsByUser(userId: String): List<String>
    fun getActiveSessionsByUser(userId: String, currentTime: Long): List<String>
}