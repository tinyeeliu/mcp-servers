# Core Module Build Commands

## Changes Made
- Removed maven-shade-plugin to eliminate uber jar issues with native builds
- Set main class to `io.mcp.core.server.SimpleSdkStdioAsyncTestServer`
- Added Main-Class manifest to regular jar for direct execution
- Added native profile with GraalVM native-maven-plugin for native builds
- Modified run_inspector.sh to use maven exec:java for core module (since it's not shaded)

## Normal JAR Build

Using build script:
```bash
./scripts/build_module.sh core
```

Direct Maven command:
```bash
mvn clean package -DskipTests -pl modules/core -am
```

## Native Image Build

Using build script:
```bash
./scripts/build_native.sh core
```

Direct Maven command:
```bash
mvn clean package -DskipTests -Pnative -pl modules/core -am
```

Note: Native builds require GraalVM to be installed and configured.
