package io.mcp.core.base;

import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

public abstract class BaseMcpTool implements McpTool {
    @Override
    public McpServerFeatures.AsyncToolSpecification getToolSpecification() {

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(getTool())
                .callHandler((exchange, request) -> {
                    try {
                        return Mono.fromFuture(call(exchange, request));
                    } catch (Exception e) {
                        return Mono.just(
                            McpSchema.CallToolResult.builder()
                                .addTextContent(e.getMessage())
                                .isError(true)
                                .build()
                        );
                    }
                })
                .build();
    }
}
