## ISSUE

Running with jvm:

./scripts/run_inspector.sh random stdio

Works fine. Stdio MCP tool works.

Running native: 

./scripts/run_native.sh random stdio

Serve log works fine, but inspector doesn't show results but doesn't show error.


## SUSPECTED CAUSE

I am suspecting that GraalVM compile striped out something.
Maybe related to json transformation.
However, stdio transport uses official SDK. I am not sure we do any custom transformation.