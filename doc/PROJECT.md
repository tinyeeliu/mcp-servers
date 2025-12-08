# Summary

MCP servers project with pure java.
Designed to be work in pure java with reactive code.

Always support stdio as base. Framework can take the stdio implementation and wrap around it with different hosting.

# Folder Structure

root
  - pom.xml
  - modules
    - core (java project)
    - random (java project)
  - scripts
  - spec

# Framework

Using maven reactor structure for this.

There's a parent pom that can build any submodule. This make sure all modules can work together without conflicting dependencies.

# Native GraalVM Support

Use Java 25 and ComputableFuture without reactive framework.

Jars can be embedded into other Java modules easily and able to run on serverless platform.

# SDK

Using Anthropics Java SDK.

https://github.com/modelcontextprotocol/java-sdk

# Dependencies

We want to miminmize dependencies to compile to native JAVA code.

1. Http - Use native Java11 http client.


## Initial Modules

1. core - interfaces and classes that can shared by all other modules
2. random - a simple mcp service that return a random Long.


#

git clone https://oauth2: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@github.com/tinyeeliu/mcp-servers.git