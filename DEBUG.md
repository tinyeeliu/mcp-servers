## ISSUE

Running with jvm:

./scripts/run_inspector.sh random stdio

Works fine. Stdio MCP tool works.

[DEBUG 2025-12-07 16:47:28.960] SimpleSdkStdioAsyncTestServer starting...
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
[DEBUG 2025-12-07 16:47:29.071] SimpleSdkStdioTestServer started - connection test mode

Running native: 

./scripts/run_native.sh core stdio

Server seems terminated immediately.

Server Log:

[DEBUG 2025-12-07 16:49:53.919] SimpleSdkStdioAsyncTestServer starting...
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
[DEBUG 2025-12-07 16:49:53.923] SimpleSdkStdioTestServer started - connection test mode
[DEBUG 2025-12-07 16:49:53.923] Shutting down SimpleSdkStdioTestServer


## SUSPECTED CAUSE

GraalVM related issue causing server shutdown immediately.
The log "Shutting down SimpleSdkStdioTestServer" is not presented when using jvm.

## ACTION TAKEN

Removed maven-shade-plugin from core module pom.xml to eliminate potential GraalVM native build issues with uber jars.
- Core module now builds as regular jar instead of shaded uber jar
- Set main class to `io.mcp.core.server.SimpleSdkStdioAsyncTestServer`
- Build and exec plugin verified working

## NEXT STEPS

Test native build with core module to see if the hanging issue is resolved.