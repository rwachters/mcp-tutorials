# MCP Tutorials (Kotlin)

Welcome to `mcp-tutorials`, a collection of educational projects demonstrating various aspects of the Model Context Protocol (MCP) using the Kotlin SDK. Each "part" in this repository focuses on a specific feature or interaction pattern of MCP.

## Part 1: Generic STDIO Client with "Hello World" Server

This first tutorial (located in the `part1` module) showcases a generalized MCP client written in Kotlin that can connect to *any* STDIO-based MCP server. For demonstration purposes, it's designed to interact with the simple "greet" tool provided by the `HelloWorldServer` from our companion `mcp-hello-world` project.

This example is ideal for understanding:
*   How to build a flexible MCP client capable of launching external servers as subprocesses.
*   Dynamic discovery and interactive invocation of server-provided tools.
*   Basic MCP client-server communication using standard I/O.

## Table of Contents

*   [What is MCP?](#what-is-mcp)
*   [Project Structure](#project-structure)
*   [Prerequisites](#prerequisites)
*   [Building the Project](#building-the-project)
*   [Running Part 1](#running-part-1)
*   [How Part 1 Works](#how-part-1-works)
*   [Troubleshooting](#troubleshooting)
*   [Further Learning](#further-learning)

## What is MCP?

The Model Context Protocol (MCP) is an open-source standard for connecting AI applications to external systems. It provides a standardized way for AI applications (like LLMs) to access data sources, tools, and workflows, enabling them to retrieve information and perform tasks in the external world.

Think of MCP as a universal adapter for AI models, allowing them to extend their capabilities beyond their internal knowledge to interact with real-world systems.

## Project Structure

This project has a multi-module Gradle setup. For this tutorial, we focus on the `part1` module:

```
mcp-tutorials/
├── build.gradle.kts          // Root Gradle configuration
├── settings.gradle.kts       // Gradle settings for multi-module project
├── part1/                    // The first tutorial module (our client)
│   ├── build.gradle.kts
│   └── src/main/kotlin/eu/torvian/mcp/tutorials/part1/
│       ├── Main.kt           // Entry point for the generic client
│       ├── ServerConfig.kt   // Data class for subprocess configuration
│       └── StdioMcpClient.kt // The generalized MCP client implementation
├── docs/                     // (Future) Additional documentation specific to these tutorials
└── gradle/
    ├── libs.versions.toml    // Version catalog for dependencies
    └── wrapper/              // Gradle wrapper files
```

**Note:** The `HelloWorldServer` used in this tutorial is from a separate project. You will need to build its JAR separately.

## Prerequisites

*   **Java 17 or later**: Required for running Kotlin applications on the JVM.
*   **Gradle**: (Optional) If you don't use the provided Gradle wrapper, ensure you have Gradle installed.
*   **Basic understanding of Kotlin**: Familiarity with Kotlin syntax and concepts will be helpful.
*   **`mcp-hello-world-server.jar`**: The server JAR from the `mcp-hello-world` project. You can obtain it by cloning and building that repository:
    ```bash
    git clone https://github.com/rwachters/mcp-hello-world.git
    cd mcp-hello-world
    ./gradlew :server:jar
    # The server JAR will be at server/build/libs/mcp-hello-world-server.jar
    cd .. # Return to mcp-tutorials root
    ```

## Building the Project

This project uses Gradle to build a "fat JAR" for the `part1` client, which includes all necessary dependencies to run independently.

1.  **Clone this repository:**
    ```bash
    git clone https://github.com/rwachters/mcp-tutorials.git
    cd mcp-tutorials
    ```
2.  **Build the `part1` client JAR:**
    ```bash
    ./gradlew :part1:jar
    ```
    This will produce `part1/build/libs/part1.jar` (or similar, depending on your build configuration).

## Running Part 1

To run `part1`, you will execute its client JAR and provide the command needed to launch an MCP server as an argument. For this tutorial, we'll use the `HelloWorldServer` JAR from the `mcp-hello-world` project.

1.  **Ensure you have built the `part1` client JAR** (as per [Building the Project](#building-the-project)).
2.  **Ensure you have the `mcp-hello-world-server.jar`** (as per [Prerequisites](#prerequisites)). For simplicity in the example below, we assume you've copied it to the root of the `mcp-tutorials` project (or you can provide its full path).
3.  **Execute the `part1` client application:**
    ```bash
    # Assuming mcp-hello-world-server.jar is in the root of mcp-tutorials
    java -jar part1/build/libs/part1.jar java -jar mcp-hello-world-server.jar
    ```
    The client will start, launch the server as a child process, connect to it, and then enter an interactive loop. The combined output from both the client and its server subprocess will appear in the same terminal:
    ```
    Client: Starting server process: java -jar mcp-hello-world-server.jar
    Client: Server process started with PID: 12345
    Starting Hello World MCP Server...
    Client: Successfully connected to MCP server.
    Client: Discovered tools from server: greet
    --- Interactive Tool Caller ---
    Type a tool name to call it, or 'quit' to exit.
    > Enter tool name: greet
    > Enter value for 'name' (string, REQUIRED): Alice
    Client: Calling tool 'greet' with arguments: {name=Alice}
    Server: Called 'greet' with name='Alice'. Responding with: 'Hello, Alice!'
    Server Response: Hello, Alice!
    > Enter tool name: quit
    Client: MCP connection closed.
    Client: Server subprocess terminated.
    ```

### Other Server Examples (Conceptual)

You can use the `part1` client with any STDIO-based MCP server by adjusting the command-line arguments. Remember to set any required environment variables in your shell *before* running the client.

*   **Generic Docker Server:**
    ```bash
    # Set your API key in the shell first (e.g., export MY_API_KEY="your_secret")
    java -jar part1/build/libs/part1.jar docker run -i --rm -e MY_API_KEY my/mcp-server-image
    ```
    (Note: `-i` is for interactive mode, `-e MY_API_KEY` tells Docker to pass the host's `MY_API_KEY` env var into the container.)

*   **UV Server Example:**
    ```bash
    # Set Python path or virtual environment activation in the shell if needed
    java -jar part1/build/libs/part1.jar uv python -m my_uv_mcp_server_module
    ```

## How Part 1 Works

*   **`mcp-hello-world-server.jar` (from `mcp-hello-world` project)**:
    *   This is a separate MCP server application.
    *   It initializes an `MCP Server` instance with basic capabilities.
    *   Defines a `Tool` named `greet` with a `name` argument, using the MCP Kotlin SDK's schema definition.
    *   Registers a lambda function for the `greet` tool that extracts the `name` argument and returns a "Hello, \[name]!" `TextContent` as a `CallToolResult`.
    *   Connects to a `StdioServerTransport`, which allows it to communicate via standard input/output streams with a client.
    *   **Important:** Its initial `println("Starting...")` message is written to `System.err` to avoid interfering with the client's MCP protocol parsing on `System.out`.

*   **`StdioMcpClient.kt` (in this `part1` module)**:
    *   Initializes an `MCP Client` instance.
    *   In `connectToServer()`, it launches the specified MCP server command (e.g., `java -jar mcp-hello-world-server.jar`) as a separate child process using `ProcessBuilder`.
    *   It then sets up a `StdioClientTransport` to communicate with the server subprocess's standard I/O streams. Any environment variables set in the client's shell environment will be inherited by the server subprocess.
    *   After connecting, it calls `mcp.listTools()` to dynamically discover all tools offered by the server.
    *   The `interactiveToolLoop()` allows the user to input a tool name and, based on the tool's `inputSchema`, prompts for necessary arguments. It then calls `mcp.callTool()` to execute the server's chosen tool.
    *   The structured `CallToolResult` from the server is then processed, and its `TextContent` (or error messages) is printed to the console.

*   **`build.gradle.kts` (Fat JARs)**:
    *   The Gradle build script for the `part1` module is configured to create a "fat JAR". This means all dependencies (like `kotlin-stdlib`, `kotlinx-coroutines-core`, and the MCP Kotlin SDK itself) are bundled directly into the `part1.jar` file. This makes it easily runnable with `java -jar`.
    *   Special `exclude` rules are applied in the `jar` task to prevent `Duplicate entry` errors during the build, particularly for `META-INF` files and `module-info.class` files that often cause conflicts when merging JARs.

## Troubleshooting

*   **`NoClassDefFoundError`**: This typically means your JAR is not a "fat JAR" and is missing runtime dependencies. Ensure you're building with the fat JAR configuration in `part1/build.gradle.kts` and using `./gradlew :part1:jar`.
*   **`Duplicate entry` during build**: Check your `part1/build.gradle.kts` `jar` task for the `from(configurations.runtimeClasspath.get()...)` block and ensure the `exclude` rules for `META-INF` files are present and correct.
*   **Client fails to connect (hangs or errors)**:
    *   Ensure the command and arguments you provide to the client for launching the server are absolutely correct and fully specify how to start the target MCP server.
    *   Verify there are no unexpected issues starting the subprocess (e.g., permissions, `java`, `docker`, `uv` commands not in your system's PATH).
    *   **Crucially**: The `StdioMcpClient` expects *only* valid MCP JSON-RPC messages on the server's `stdout`. If the server prints *any* plain text (e.g., startup logs, warnings) to `stdout` before or during the MCP handshake, the client's MCP parser will likely fail or hang. Ensure such output is directed to `stderr` by the server. (Our `HelloWorldServer` does this correctly by writing startup messages to `System.err`).
*   **SLF4J warnings**: `SLF4J(W): No SLF4J providers were found.` is a common warning. SLF4J is a logging facade. To remove the warning, you can add a logging implementation like `slf4j-simple` to your `part1/build.gradle.kts` dependencies (e.g., `implementation("org.slf4j:slf4j-simple:2.0.13")`). This is purely for logging output and doesn't affect the core MCP functionality.

## Further Learning

*   **Model Context Protocol Documentation**: [https://modelcontextprotocol.io/introduction](https://modelcontextprotocol.io/introduction)
*   **MCP Kotlin SDK GitHub**: [https://github.com/modelcontextprotocol/kotlin-sdk](https://github.com/modelcontextprotocol/kotlin-sdk)
*   **Kotlin Coroutines Documentation**: [https://kotlinlang.org/docs/coroutines-guide.html](https://kotlinlang.org/docs/coroutines-guide.html)
*   **Related Project: MCP Hello World (Kotlin)**: For the source code of the `HelloWorldServer` used in this tutorial, and a simpler client/server example, visit [https://github.com/rwachters/mcp-hello-world](https://github.com/rwachters/mcp-hello-world).
