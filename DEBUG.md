## ISSUE

Running with jvm:

./scripts/run_inspector.sh random http

Works fine. Stdio MCP tool works.

Running native: 

./scripts/run_native.sh random http

Most endpoint works, but tools/list doesn't work.

Server:
[DEBUG 2025-12-06 20:53:59.778] --- Processing JSON-RPC method: tools/list id: 4
[DEBUG 2025-12-06 20:53:59.778]     Handling tools/list request
[DEBUG 2025-12-06 20:53:59.778]     Listing 1 tools
[DEBUG 2025-12-06 20:53:59.778]       Tool: generateRandom - Generates a random integer between 0 (inclusive) and the specified bound (exclusive)
[DEBUG 2025-12-06 20:53:59.785]     tools/list response prepared
[DEBUG 2025-12-06 20:53:59.785] <<< Response: {"jsonrpc":"2.0","id":4,"result":{"tools":[{"name":"generateRandom","description":"Generates a random integer between 0 (inclusive) and the specified bound (exclusive)","inputSchema":{}}]}}

Client:
Uncaught (in promise) ZodError: [
  {
    "code": "invalid_literal",
    "expected": "object",
    "path": [
      "tools",
      0,
      "inputSchema",
      "type"
    ],
    "message": "Invalid literal value, expected \"object\""
  }
]
    at get error (index-Dg7mXHRE.js:11876:23)
    at ZodObject.parse (index-Dg7mXHRE.js:11951:18)
    at index-Dg7mXHRE.js:23698:39
    at Client._onresponse (index-Dg7mXHRE.js:23625:7)
    at _transport.onmessage (index-Dg7mXHRE.js:23505:14)
    at processStream (index-Dg7mXHRE.js:24750:77)




## UNLIKELY CAUSE

Server is not stopped. I can see server log when inspector clicks continue.



## SUSPECTED CAUSE

I am suspecting that GraalVM compile striped out something.
Maybe related to json transformation.
However, stdio transport uses official SDK. I am not sure we do any custom transformation.

## ACTION

Think about what can cause native build to not work.
Can add more debugging log with Utility.debug.