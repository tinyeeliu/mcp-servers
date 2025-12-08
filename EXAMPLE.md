# MCP ENDPOINTS LIST

This document lists all supported MCP endpoints and their example JSON inputs for HTTP server testing.
The MCP server supports both Streamable HTTP Transport and SSE Transport.

**Available Modules:** Replace `{module}` in paths with one of: `random`, `root`

## HTTP ENDPOINTS

### Streamable HTTP Transport
- **POST /{module}/mcp** - Handles all JSON-RPC requests for each module
- **Accept: text/event-stream** header can be used to receive responses as SSE streams

### SSE Transport (Legacy)
- **GET /{module}/sse** - Establishes SSE connection, returns endpoint URL
- **POST /{module}/messages?sessionId=xxx** - Sends messages to server

## MCP JSON-RPC METHODS

### initialize
Initializes the MCP connection and returns server capabilities.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {},
      "prompts": {},
      "resources": {}
    },
    "clientInfo": {
      "name": "test-client",
      "version": "1.0.0"
    }
  }
}
```

### initialized
Notification sent after successful initialization.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "method": "initialized",
  "params": {}
}
```

### ping
Health check endpoint.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "ping",
  "params": {}
}
```

### tools/list
Lists all available tools.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/list",
  "params": {}
}
```

### tools/call
Executes a specific tool.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "generateRandom",
    "arguments": {
      "bound": 100
    }
  }
}
```

### prompts/list
Lists all available prompts.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "prompts/list",
  "params": {}
}
```

### prompts/get
Gets a specific prompt by name.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "prompts/get",
  "params": {
    "name": "random_game"
  }
}
```

### resources/list
Lists all available resources.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "resources/list",
  "params": {}
}
```

### resources/read
Reads a specific resource by URI.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 8,
  "method": "resources/read",
  "params": {
    "uri": "https://en.wikipedia.org/wiki/Random_number_generation"
  }
}
```

### resources/templates/list
Lists all available resource templates.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 9,
  "method": "resources/templates/list",
  "params": {}
}
```

### resources/templates/read
Reads from a resource template.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "resources/templates/read",
  "params": {
    "uriTemplate": "file:///{path}"
  }
}
```

## TOOLS

### generateRandom
Generates a random integer between 0 (inclusive) and the specified bound (exclusive).

**Input Schema:**
```json
{
  "bound": {
    "type": "integer",
    "description": "The upper bound (exclusive) for the random number. Must be positive."
  }
}
```

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 11,
  "method": "tools/call",
  "params": {
    "name": "generateRandom",
    "arguments": {
      "bound": 100
    }
  }
}
```

## PROMPTS

### random_game
Generate a random number for gaming scenarios like dice rolls, lottery picks, or game mechanics.

**Arguments:**
- `bound` (required): The upper bound for the random number (e.g., 6 for a die, 100 for percentage, 52 for deck of cards)

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 12,
  "method": "prompts/get",
  "params": {
    "name": "random_game",
    "arguments": {
      "bound": "6"
    }
  }
}
```

### random_test
Generate a random number for testing scenarios like sample data generation, random IDs, or statistical testing.

**Arguments:**
- `bound` (required): The upper bound for the random number (e.g., 1000 for test IDs, 100 for percentages)

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 13,
  "method": "prompts/get",
  "params": {
    "name": "random_test",
    "arguments": {
      "bound": "1000"
    }
  }
}
```

## RESOURCES

### Random Number Generation Information
URI: `https://en.wikipedia.org/wiki/Random_number_generation`

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 14,
  "method": "resources/read",
  "params": {
    "uri": "https://en.wikipedia.org/wiki/Random_number_generation"
  }
}
```

## RESOURCE TEMPLATES

### Project Files
URI Template: `file:///{path}` - Access files in the project directory.

Path: `POST /{module}/mcp`

Example input:
```json
{
  "jsonrpc": "2.0",
  "id": 15,
  "method": "resources/templates/read",
  "params": {
    "uriTemplate": "file:///{path}"
  }
}
```