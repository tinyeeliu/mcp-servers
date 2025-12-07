## ISSUE

Running with jvm:

./scripts/run_inspector.sh random stdio

Works fine. Stdio MCP tool works.

Running native: 

./scripts/run_native.sh random stdio

Server seems hang.

Server Log:

New STDIO connection request
Query parameters: {"command":"modules/random/target/mcp-random","args":"stdio","env":"{\"HOME\":\"/Users/peterliu\",\"LOGNAME\":\"peterliu\",\"PATH\":\"/Users/peterliu/.npm/_npx/87097e74bd56c758/node_modules/.bin:/Users/peterliu/workspace/mcp-servers/node_modules/.bin:/Users/peterliu/workspace/node_modules/.bin:/Users/peterliu/node_modules/.bin:/Users/node_modules/.bin:/node_modules/.bin:/Users/peterliu/.nvm/versions/node/v22.14.0/lib/node_modules/npm/node_modules/@npmcli/run-script/lib/node-gyp-bin:/Users/peterliu/.pyenv/shims:/Users/peterliu/.nvm/versions/node/v22.14.0/bin:/Users/peterliu/.codeium/windsurf/bin:/opt/homebrew/opt/postgresql@16/bin:/opt/homebrew/opt/gradle@6/bin:/Users/peterliu/workspace/google-cloud-sdk/bin:/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/System/Cryptexes/App/usr/bin:/usr/bin:/bin:/usr/sbin:/sbin:/var/run/com.apple.security.cryptexd/codex.system/bootstrap/usr/local/bin:/var/run/com.apple.security.cryptexd/codex.system/bootstrap/usr/bin:/var/run/com.apple.security.cryptexd/codex.system/bootstrap/usr/appleinternal/bin:/Library/Apple/usr/bin:/Users/peterliu/.nvm/versions/node/v22.14.0/bin:/Users/peterliu/.codeium/windsurf/bin:/opt/homebrew/opt/postgresql@16/bin:/opt/homebrew/opt/gradle@6/bin:/Users/peterliu/workspace/google-cloud-sdk/bin:/Users/peterliu/.local/bin:/Users/peterliu/.cache/lm-studio/bin:/Users/peterliu/workspace/flutter-sdk/bin:/Users/peterliu/.cursor/extensions/vscjava.vscode-java-debug-0.58.3-universal/bundled/scripts/noConfigScripts:/Users/peterliu/.local/bin:/Users/peterliu/.cache/lm-studio/bin:/Users/peterliu/.local/bin:/Users/peterliu/workspace/flutter-sdk/bin\",\"SHELL\":\"/bin/zsh\",\"TERM\":\"xterm-256color\",\"USER\":\"peterliu\"}","transportType":"stdio"}
STDIO transport: command=modules/random/target/mcp-random, args=stdio
Created client transport
Created server transport
Received POST message for sessionId f4c03941-4412-4a1b-8dfb-7a63ceb234aa

Client Log:

event: endpoint
data: /message?sessionId=1e6cb187-cb2d-45df-9d18-6b7d30264046

event: message
data: {"jsonrpc":"2.0","method":"notifications/message","params":{"level":"info","logger":"stdio","data":{"message":"SLF4J(W): No SLF4J providers were found.\nSLF4J(W): Defaulting to no-operation (NOP) logger implementation\nSLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details."}}}




## UNLIKELY CAUSE

Server is not stopped. I can see server log when inspector clicks continue.



## SUSPECTED CAUSE

GraalVM related issue.

## ACTION TAKEN

Removed maven-shade-plugin from core module pom.xml to eliminate potential GraalVM native build issues with uber jars.
- Core module now builds as regular jar instead of shaded uber jar
- Set main class to `io.mcp.core.server.SimpleSdkStdioAsyncTestServer`
- Build and exec plugin verified working

## NEXT STEPS

Test native build with core module to see if the hanging issue is resolved.