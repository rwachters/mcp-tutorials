package eu.torvian.mcp.tutorials.part1

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.iterator

/**
 * This class represents a generalized MCP (Model Context Protocol) Client in Kotlin
 * designed to interact with any STDIO-based MCP server by launching it as a subprocess.
 *
 * It connects to a local server, discovers its tools, and allows interactive execution of them.
 */
class StdioMcpClient : AutoCloseable {
    // 1. Initialize the core MCP Client instance.
    // The client needs to identify itself to the server during the connection handshake.
    private val mcp: Client =
        Client(clientInfo = Implementation(name = "stdio-mcp-client", version = "1.0.0")) // Updated client name
    // A local cache to store the tools discovered from the connected MCP Server.
    // The key is the tool's name, and the value is the Tool definition.
    private lateinit var availableTools: Map<String, Tool>
    // Keep a reference to the launched server process to manage its lifecycle.
    private var serverProcess: Process? = null

    /**
     * Connects this MCP Client to an MCP Server.
     *
     * Launches the server as a subprocess using the provided configuration and communication occurs
     * via standard input/output (STDIO). After connection, the client discovers the
     * tools offered by the server.
     *
     * @param config The configuration specifying how to launch the server subprocess.
     */
    suspend fun connectToServer(config: ServerConfig) {
        try {
            // Build the command array to launch the server.
            // ProcessBuilder expects the command and its arguments as a single list.
            val fullCommand = listOf(config.command) + config.args

            // Start the server application as a new subprocess.
            println("Client: Starting server process: ${fullCommand.joinToString(" ")}")
            val processBuilder = ProcessBuilder(fullCommand)
//                .redirectErrorStream(true) // Redirect server's stderr to client's stdout for debugging

            // Apply environment variables
            processBuilder.environment().putAll(config.env)

            serverProcess = processBuilder.start()
            println("Client: Server process started with PID: ${serverProcess?.pid()}")

            // Setup STDIO (Standard Input/Output) transport.
            // This transport uses the input and output streams of the launched subprocess
            // to send and receive MCP messages.
            val transport = StdioClientTransport(
                input = serverProcess!!.inputStream.asSource().buffered(),  // Client reads server's stdout.
                output = serverProcess!!.outputStream.asSink().buffered(), // Client writes to server's stdin.
            )
            // Connect the MCP client instance to the configured transport.
            // This initiates the MCP handshake and establishes the communication session.
            mcp.connect(transport)
            println("Client: Successfully connected to MCP server.")
            // Request the list of available tools from the connected server.
            // This is how the client discovers what capabilities the server offers.
            val toolsResult = mcp.listTools()
            availableTools = toolsResult.tools.associateBy { it.name } // Store them in our map.
            println("Client: Discovered tools from server: ${availableTools.keys.joinToString(", ")}")
        } catch (e: Exception) {
            println("Client: Failed to connect to MCP server: $e")
            serverProcess?.destroyForcibly() // Ensure the subprocess is killed if connection fails
            throw e
        }
    }

    /**
     * Runs an interactive loop, allowing the user to input tool names and arguments
     * to call tools on the connected MCP Server.
     *
     * This simulates how an AI application might decide to use a tool based on a user's request.
     */
    suspend fun interactiveToolLoop() {
        println("\n--- Interactive Tool Caller ---")
        println("Type a tool name to call it, or 'quit' to exit.")
        if (availableTools.isEmpty()) {
            println("No tools discovered from the server. Exiting interactive loop.")
            return
        }

        while (true) {
            print("\n> Enter tool name: ")
            val toolName = readlnOrNull()?.trim() ?: break // Read user input for tool name.
            if (toolName.equals("quit", ignoreCase = true)) break // Exit loop on "quit".

            val tool = availableTools[toolName]
            if (tool == null) {
                println("Unknown tool '$toolName'. Available tools are: ${availableTools.keys.joinToString(", ")}")
                continue // Ask for input again if tool not found.
            }

            // Dynamically prompt for arguments based on the tool's inputSchema.
            // This is a simplified implementation, only supporting basic string properties.
            // A real-world client would use a full JSON Schema parser and potentially
            // a more sophisticated UI for argument input (e.g., number fields, dropdowns, etc.).
            val arguments = mutableMapOf<String, Any?>() // Use Any? to allow different types if expanded
            val inputSchemaProperties: JsonObject = tool.inputSchema.properties
            val requiredArguments = tool.inputSchema.required ?: emptyList()

            for ((propName, propSchemaJsonElement) in inputSchemaProperties) {
                // Ensure propSchemaJsonElement is a JsonObject if it represents a schema definition
                val propSchema = propSchemaJsonElement as? JsonObject ?: continue
                val propType = (propSchema["type"] as? JsonPrimitive)?.content
                val propDescription = (propSchema["description"] as? JsonPrimitive)?.content
                val isRequired = requiredArguments.contains(propName)

                // Simple handling for string types.
                // For other types (number, boolean, object, array), a more sophisticated
                // input mechanism and parsing would be needed.
                if (propType == "string") {
                    print("> Enter value for '$propName' (string, ${if (isRequired) "REQUIRED" else "OPTIONAL"}${propDescription?.let { " - $it" } ?: ""}): ")
                    val value = readlnOrNull() ?: ""
                    if (value.isNotBlank() || !isRequired) {
                        arguments[propName] = value
                    } else if (value.isBlank()) {
                        println("Error: Required argument '$propName' cannot be empty. Skipping tool call.")
                        // If a required argument is empty, we cannot proceed meaningfully.
                        // For this example, we'll break and re-ask for tool name.
                        return // Skip this entire tool invocation
                    }
                } else {
                    println("Warning: Argument '$propName' has type '$propType', which is not interactively supported by this client yet.")
                    if (isRequired) {
                        println("Error: Required argument '$propName' cannot be provided. Skipping tool call.")
                        return // Skip this entire tool invocation
                    }
                    // For now, skip unsupported types or add a placeholder.
                    // A real client would need to parse full JSON schema for complex types.
                }
            }

            println("Client: Calling tool '$toolName' with arguments: $arguments")
            // Execute the tool on the server. `mcp.callTool` sends the request
            // and waits for the server's `CallToolResult`.
            val result = mcp.callTool(name = toolName, arguments = arguments)
            // Process and print the text content from the tool's response.
            val resultText = result?.content
                ?.filterIsInstance<TextContent>() // Filter for text content objects.
                ?.joinToString("\n") { it.text.toString() } // Extract the text.

            if (result?.isError == true) {
                println("Server responded with an ERROR during tool execution:")
                println("Error Content: $resultText")
                // If structuredContent contains error details, print it too
                result.structuredContent?.let {
                    println("Structured Error: $it")
                }
            } else {
                println("Server Response: $resultText")
            }
        }
    }

    /**
     * Closes the MCP client connection and any associated resources.
     *
     * This is crucial for proper resource management and terminating the server subprocess.
     */
    override fun close() {
        runBlocking {
            mcp.close() // Close the MCP connection.
            println("Client: MCP connection closed.")
            // Terminate the server subprocess.
            serverProcess?.destroy() // Attempts a graceful termination.
            serverProcess?.waitFor() // Wait for it to exit (e.g., server cleans up).
            if (serverProcess?.isAlive == true) {
                println("Client: Server process did not terminate gracefully. Forcibly destroying.")
                serverProcess?.destroyForcibly() // Forcibly kill if it's still alive.
                serverProcess?.waitFor() // Wait again for forced termination.
            }
            println("Client: Server subprocess terminated.")
        }
    }
}