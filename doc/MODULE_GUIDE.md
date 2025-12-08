# Module Authoring Guide

This guide shows how to add a new MCP module. The `random` module is a minimal, working reference you can copy.

## What a module must provide
- A Maven submodule under `modules/<name>` that inherits the root parent POM.
- A service class extending `BaseMcpService` that declares module metadata and returns the tools it offers.
- One or more tool classes extending `BaseMcpTool` that implement `call`.
- Spec assets under `src/main/resources/io/mcp/spec/<module>/` for tools, prompts, resources, and templates.
- Service registration in `src/main/resources/META-INF/services/io.mcp.core.protocol.McpService`.

## Quick recipe
1) Create the Maven module  
   - Copy `modules/random/pom.xml` and adjust `artifactId`, `<name>`, `<description>`, and the exec plugin `commandlineArgs` (use `http <module>` by default).
2) Implement the service  
   - Put it in `src/main/java/io/mcp/<module>/service/<Module>Service.java`.  
   - Extend `BaseMcpService`; implement:
     - `getServerInfo()` → return an `Implementation` with a module-specific name/version.  
     - `getTools()` → return a list of instantiated tool classes.  
     - `getModule()` → return the module slug (e.g., `"random"`).
3) Implement at least one tool  
   - Place tools in `src/main/java/io/mcp/<module>/tool/`.  
   - Extend `BaseMcpTool`; implement:
     - `getName()` → tool name (matches the spec file).  
     - `getModule()` → module slug.  
     - `call(...)` → validate inputs, perform work, and return a `CallToolResult` with `addTextContent` (or other content types) and `isError` as needed.
4) Add MCP spec files (JSON)  
   - Tool spec: `spec/<module>/tool/<toolName>.json` describes the tool and its input schema.  
   - Prompts: `spec/<module>/prompt/<toolName>.json` with suggested prompt presets.  
   - Resources: `spec/<module>/resource/<toolName>.json` with external URIs.  
   - Templates: `spec/<module>/template/<toolName>.json` with `uriTemplate` entries.  
   - Keep names in these files aligned with `getName()`/`getModule()`.
5) Register the service  
   - Append the fully qualified service class name to `META-INF/services/io.mcp.core.protocol.McpService`.
6) (Optional) Native image metadata  
   - Copy the files under `META-INF/native-image` from `random` if you need GraalVM native-image support.
7) Tests  
   - Add transport and tool-behavior tests under `src/test/java/io/mcp/<module>/`.

## MCP endpoints and specs (quick mental model)
- Tools
  - `tools/list` advertises tools (name/description/schema).
  - `tools/call` runs a tool with arguments validated against its JSON schema.
  - Spec files live in `spec/<module>/tool/*.json` and define `name`, `description`, and `inputSchema`.
- Prompts
  - `prompts/list` discovers prompt presets; `prompts/get` returns a prompt by name (parameterized if needed).
  - Specs in `spec/<module>/prompt/*.json` declare titles, descriptions, and argument requirements.
- Resources
  - `resources/list` advertises external URIs; `resources/read` fetches one.
  - Specs in `spec/<module>/resource/*.json` list URIs with metadata (`name`, `title`, `mimeType`).
- Templates
  - `resources/templates/list` and `resources/templates/read` expose URI templates that clients can fill.
  - Specs in `spec/<module>/template/*.json` declare `uriTemplate` entries plus human-friendly names.
- Keep tool names and module slugs consistent across Java classes (`getName()`, `getModule()`) and all JSON specs so discovery and validation line up.

## Random module as a template
- Service (`RandomService`) demonstrates the minimal overrides:
```java
public class RandomService extends BaseMcpService {
    @Override public Implementation getServerInfo() { return new Implementation("mcp-random-server", "1.0.0"); }
    @Override public List<McpTool> getTools() { return List.of(new GenerateRandom()); }
    @Override public String getModule() { return "random"; }
}
```
- Tool (`GenerateRandom`) shows validation and result shaping:
```java
public class GenerateRandom extends BaseMcpTool {
    @Override public String getName() { return "generateRandom"; }
    @Override public String getModule() { return "random"; }
    @Override
    public CompletableFuture<McpSchema.CallToolResult> call(McpAsyncServerExchange ex, CallToolRequest req) {
        int bound = ((Number) req.arguments().get("bound")).intValue();
        if (bound <= 0) throw new IllegalArgumentException("bound must be a positive integer");
        int result = new Random().nextInt(bound);
        return CompletableFuture.completedFuture(
            McpSchema.CallToolResult.builder().addTextContent(String.valueOf(result)).isError(false).build()
        );
    }
}
```
- Spec assets live under `src/main/resources/io/mcp/spec/random/`:
  - Tool schema: `tool/generateRandom.json`
  - Prompts: `prompt/generateRandom.json` (two presets)
  - Resources: `resource/generateRandom.json` (Wikipedia link)
  - Templates: `template/generateRandom.json` (file URI template)
- Service is registered at `META-INF/services/io.mcp.core.protocol.McpService` with `io.mcp.random.service.RandomService`.

## Running and testing
- Run just one module (exec plugin uses the module name):  
  `mvn -pl modules/random exec:java` (starts `StandaloneLauncher` with `http random`). Replace `random` with your module slug.
- Run tests for a module:  
  `mvn -pl modules/random test`
- `RandomNumberServerTest` illustrates exercising stdio, HTTP, and SSE transports, plus tool validation; copy the structure for new modules.

## Pre-flight checklist
- [ ] POM inherits the root parent and has exec args pointing at your module.
- [ ] Service class implements `getServerInfo`, `getTools`, `getModule`.
- [ ] All tools implement `getName`, `getModule`, and input validation in `call`.
- [ ] JSON specs (tool/prompt/resource/template) match tool names and arguments.
- [ ] Service is listed in `META-INF/services/io.mcp.core.protocol.McpService`.
- [ ] Tests cover at least one happy path and one validation/error path.
