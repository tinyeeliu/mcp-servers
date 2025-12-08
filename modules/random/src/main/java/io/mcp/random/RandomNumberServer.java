package io.mcp.random;

import io.mcp.core.protocol.McpService;
import io.mcp.core.server.StandaloneLauncher;
import io.mcp.core.utility.Utility;
import io.mcp.random.service.RandomService;

public class RandomNumberServer {

    public static void main(String[] args) throws Exception {

        String transport = null;

        if (args.length > 0) {
            transport = args[0];
        }

         
        Utility.setDebug(true);
        /* 
        Utility.redirectStdErrToLog();
        */

        McpService service = new RandomService();
        StandaloneLauncher.launch(transport, service);

    }

}
