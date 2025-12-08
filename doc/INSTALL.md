# StreamableServer Integration Guide

This guide shows how to integrate `StreamableServer` with various Java frameworks and platforms.

## Overview

`StreamableServer` provides HTTP request/response handling for the MCP (Model Context Protocol) **without embedding an HTTP server**. It can be integrated with any HTTP framework or serverless platform.

## Basic Setup

First, add the core module dependency to your project:

```xml
<dependency>
    <groupId>io.mcp</groupId>
    <artifactId>mcp-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Then create and initialize the server:

```java
import io.mcp.core.server.StreamableServer;
import io.mcp.core.protocol.McpService;

StreamableServer server = new StreamableServer();
server.initialize(myMcpService);
```

---

## Framework Integration Examples

### 1. Plain Java HTTP Server (JDK Built-in)

For simple use cases with Java's built-in HTTP server:

```java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;

public class McpHttpServer {
    public static void main(String[] args) throws Exception {
        StreamableServer mcpServer = new StreamableServer();
        mcpServer.initialize(myMcpService);
        
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/mcp", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                String response = mcpServer.handleRequestSync(requestBody, null);
                
                exchange.getResponseHeaders().set("Content-Type", mcpServer.getContentType());
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        });
        httpServer.start();
        System.out.println("MCP Server running on http://localhost:8080/mcp");
    }
}
```

---

### 2. Jakarta Servlet (Tomcat, Jetty, WildFly)

For deployment to any Servlet container:

```java
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/mcp")
public class McpServlet extends HttpServlet {
    
    private StreamableServer mcpServer;
    
    @Override
    public void init() {
        mcpServer = new StreamableServer();
        mcpServer.initialize(getMyMcpService());
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType(mcpServer.getContentType());
        
        // Use session ID for stateful connections (optional)
        String sessionId = req.getSession().getId();
        
        mcpServer.handleRequest(req.getReader(), resp.getWriter(), sessionId);
    }
    
    @Override
    public void destroy() {
        if (mcpServer != null) {
            mcpServer.shutdown();
        }
    }
}
```

**Maven dependency for Jakarta Servlet:**
```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>
</dependency>
```

---

### 3. Spring Boot

For Spring Boot applications:

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/mcp")
public class McpController {
    
    private final StreamableServer mcpServer;
    
    public McpController(McpService mcpService) {
        this.mcpServer = new StreamableServer();
        this.mcpServer.initialize(mcpService);
    }
    
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<String> handleMcp(@RequestBody String body) {
        return Mono.fromFuture(mcpServer.handleRequest(body, null));
    }
}
```

**Or with WebFlux for fully reactive:**

```java
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Configuration
public class McpRouter {
    
    @Bean
    public RouterFunction<ServerResponse> mcpRoutes(McpService mcpService) {
        StreamableServer server = new StreamableServer();
        server.initialize(mcpService);
        
        return RouterFunctions.route()
            .POST("/mcp", request -> 
                request.bodyToMono(String.class)
                    .flatMap(body -> Mono.fromFuture(server.handleRequest(body, null)))
                    .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response)))
            .build();
    }
}
```

---

### 4. Quarkus

For Quarkus applications:

**Using JAX-RS (RESTEasy):**

```java
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;

@Path("/mcp")
@ApplicationScoped
public class McpResource {
    
    private final StreamableServer mcpServer;
    
    @Inject
    public McpResource(McpService mcpService) {
        this.mcpServer = new StreamableServer();
        this.mcpServer.initialize(mcpService);
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> handleMcp(String body) {
        return Uni.createFrom()
            .completionStage(mcpServer.handleRequest(body, null));
    }
}
```

**Using Quarkus Reactive Routes (Vert.x):**

```java
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@RouteBase(path = "/mcp")
public class McpVertxResource {
    
    private final StreamableServer mcpServer;
    
    @Inject
    public McpVertxResource(McpService mcpService) {
        this.mcpServer = new StreamableServer();
        this.mcpServer.initialize(mcpService);
    }
    
    @Route(methods = Route.HttpMethod.POST, path = "")
    public void handleMcp(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            mcpServer.handleRequest(buffer.toString(), null)
                .thenAccept(response -> {
                    ctx.response()
                        .putHeader("Content-Type", mcpServer.getContentType())
                        .end(response);
                });
        });
    }
}
```

**Quarkus `application.properties`:**
```properties
# Optional: Configure HTTP settings
quarkus.http.port=8080
quarkus.http.root-path=/api
```

---

### 5. Micronaut

For Micronaut applications:

```java
import io.micronaut.http.annotation.*;
import io.micronaut.http.MediaType;
import reactor.core.publisher.Mono;
import jakarta.inject.Inject;

@Controller("/mcp")
public class McpController {
    
    private final StreamableServer mcpServer;
    
    @Inject
    public McpController(McpService mcpService) {
        this.mcpServer = new StreamableServer();
        this.mcpServer.initialize(mcpService);
    }
    
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public Mono<String> handleMcp(@Body String body) {
        return Mono.fromFuture(mcpServer.handleRequest(body, null));
    }
}
```

---

### 6. AWS Lambda

For serverless deployment on AWS Lambda:

```java
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class McpLambdaHandler implements 
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final StreamableServer mcpServer;
    
    public McpLambdaHandler() {
        this.mcpServer = new StreamableServer();
        this.mcpServer.initialize(createMcpService());
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        
        String response = mcpServer.handleRequestSync(request.getBody(), null);
        
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(Map.of("Content-Type", mcpServer.getContentType()))
            .withBody(response);
    }
    
    private McpService createMcpService() {
        // Initialize your MCP service here
        return new MyMcpService();
    }
}
```

**AWS SAM template (`template.yaml`):**
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31

Resources:
  McpFunction:
    Type: AWS::Serverless::Function
    Properties:
      Handler: com.example.McpLambdaHandler::handleRequest
      Runtime: java21
      MemorySize: 512
      Timeout: 30
      Events:
        McpApi:
          Type: Api
          Properties:
            Path: /mcp
            Method: POST
```

---

### 7. Azure Functions

For serverless deployment on Azure:

```java
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

public class McpFunction {
    
    private static final StreamableServer mcpServer;
    
    static {
        mcpServer = new StreamableServer();
        mcpServer.initialize(createMcpService());
    }
    
    @FunctionName("mcp")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "mcp"
            ) HttpRequestMessage<String> request,
            final ExecutionContext context) {
        
        String response = mcpServer.handleRequestSync(request.getBody(), null);
        
        return request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", mcpServer.getContentType())
            .body(response)
            .build();
    }
}
```

---

### 8. Google Cloud Functions

For serverless deployment on GCP:

```java
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.BufferedWriter;

public class McpCloudFunction implements HttpFunction {
    
    private static final StreamableServer mcpServer;
    
    static {
        mcpServer = new StreamableServer();
        mcpServer.initialize(createMcpService());
    }
    
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String requestBody = new String(request.getInputStream().readAllBytes());
        String responseBody = mcpServer.handleRequestSync(requestBody, null);
        
        response.setContentType(mcpServer.getContentType());
        BufferedWriter writer = response.getWriter();
        writer.write(responseBody);
    }
}
```

---

## SSE (Server-Sent Events) Support

For frameworks that support SSE streaming:

```java
// Create SSE stream for a session
String sessionId = UUID.randomUUID().toString();

Runnable closeStream = mcpServer.createSseStream(sessionId, event -> {
    // Send SSE event to client
    sseEmitter.send(event);
});

// Later, close the stream
closeStream.run();
mcpServer.closeSession(sessionId);
```

---

## GraalVM Native Image

The `StreamableServer` is designed to be compatible with GraalVM native image compilation. No reflection configuration is needed for the core server logic.

For native image builds, ensure your framework's native support is properly configured (Spring Native, Quarkus Native, Micronaut AOT, etc.).

---

## Testing

Test your MCP endpoint using curl:

```bash
# Initialize
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'

# List tools
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# Call a tool
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"generateRandom","arguments":{"bound":100}}}'
```

---

## Notes

- The `StreamableServer` is thread-safe and can handle concurrent requests
- Session management is optional - pass `null` for stateless operation
- The server follows the MCP 2024-11-05 protocol version
- All responses are JSON-RPC 2.0 compliant

