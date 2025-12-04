package io.mcp.random.service;

import io.modelcontextprotocol.spec.McpSchema.Implementation;

public class RandomService {
    

    public Implementation getServerInfo() {
        Implementation result =new Implementation("mcp-random-server", "1.0.0");
        return result;
    }
}
