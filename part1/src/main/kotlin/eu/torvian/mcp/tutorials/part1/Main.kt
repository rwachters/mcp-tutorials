package eu.torvian.mcp.tutorials.part1

import kotlinx.coroutines.runBlocking

/**
 * The main entry point for the MCP Hello World Client application.
 *
 * This function handles command-line arguments, initializes the [StdioMcpClient],
 * connects it to an MCP Server, and starts the interactive tool-calling loop.
 */
fun main(args: Array<String>) = runBlocking {
    // Check if at least the command argument is provided.
    if (args.isEmpty()) {
        println("Usage: java -jar <client.jar> <command> [command_args...]")
        println("\nExamples:")
        println("  Local Kotlin Server:   java -jar client.jar java -jar server.jar")
        println("  Generic Docker Server: java -jar client.jar docker run -i --rm -e MY_API_KEY my/mcp-server-image")
        println("                         (Note: Any necessary environment variables like MY_API_KEY must be set in your shell environment before running.)")
        println("  UV Server Example:     java -jar client.jar uv python -m my_uv_mcp_server_module")
        println("                         (Note: Any necessary environment variables for 'uv' or your Python module must be set in your shell environment.)")
        return@runBlocking // Exit if no argument.
    }

    val command = args[0]
    val commandArgs = args.drop(1) // All arguments after the command are treated as command_args

    // No specific server-type warnings or special handling.
    // The user is responsible for providing the correct command and setting environment variables.

    val serverConfig = ServerConfig(
        command = command,
        args = commandArgs
    )

    val client = StdioMcpClient() // Renamed from HelloWorldClient

    // Use a `use` block to ensure `close()` is called automatically when the client is no longer needed,
    // even if exceptions occur. This handles resource cleanup for the AutoCloseable client.
    client.use {
        client.connectToServer(serverConfig) // Establish connection to the server using the dynamically created config.
        client.interactiveToolLoop()       // Start the interactive loop for calling tools.
    }
}