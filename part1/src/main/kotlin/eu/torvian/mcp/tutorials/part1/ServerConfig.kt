package eu.torvian.mcp.tutorials.part1

/**
 * Data class to hold server configuration for STDIO servers launched as subprocesses.
 *
 * @property command The executable command (e.g., "java", "docker", "uv")
 * @property args Arguments for the command
 * @property env Environment variables for the subprocess
 */
data class ServerConfig(
    val command: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap()
)

