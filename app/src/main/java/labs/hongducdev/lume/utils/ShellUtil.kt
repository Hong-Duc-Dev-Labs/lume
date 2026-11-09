package labs.hongducdev.lume.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object ShellUtil {
    private const val TAG = "ShellUtil"
    private const val TIMEOUT_SECONDS = 10L

    suspend fun runRoot(command: String): Boolean =
        withContext(Dispatchers.IO) {
            execute(arrayOf("su", "-c", command))?.exitCode == 0
        }

    suspend fun runCommand(command: String): String =
        withContext(Dispatchers.IO) {
            execute(arrayOf("sh", "-c", command))?.output.orEmpty()
        }

    private class Result(
        val exitCode: Int,
        val output: String,
    )

    /** Chạy lệnh với stderr gộp vào stdout (tránh nghẽn pipe) và giới hạn thời gian chờ. */
    private fun execute(args: Array<String>): Result? {
        var process: Process? = null
        return try {
            process = ProcessBuilder(*args).redirectErrorStream(true).start()
            val output = StringBuilder()
            val reader =
                Thread {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { output.append(it).append("\n") }
                    }
                }.also { it.start() }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(TAG, "Command timed out: ${args.last()}")
                process.destroyForcibly()
                return null
            }
            reader.join(1000)
            Result(process.exitValue(), output.toString().trim())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to run command: ${args.last()}", e)
            process?.destroyForcibly()
            null
        }
    }
}
