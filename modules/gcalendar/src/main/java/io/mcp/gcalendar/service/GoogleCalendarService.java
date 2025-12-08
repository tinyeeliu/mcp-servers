package io.mcp.random.service;

import java.util.List;

import io.mcp.core.base.BaseMcpService;
import io.mcp.core.protocol.McpTool;
import io.mcp.core.utility.Utility;
import io.mcp.random.tool.GenerateRandom;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

public class GoogleCalendarService extends BaseMcpService{


    public GoogleCalendarService() {
        Utility.debug("GoogleCalendarService constructor");
    }
  
    @Override
    public Implementation getServerInfo() {
        Implementation result = new Implementation("mcp-gcalendar-server", "1.0.0");
        return result;
    }



    @Override
    public List<McpTool> getTools() {
        return List.of(new GenerateRandom());
    }

    @Override
    public String getModule() {
        return "gcalendar";
    }



}
