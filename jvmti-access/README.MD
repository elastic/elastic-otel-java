This project provides access to native functionality from within java, especially to `JVMTI`.
It is implemented by loading a native library. The sources of the native library can be found in `src/main/jni`.

## Building

The native library can be built via `./gradlew :jvmti-access:compileJni`.
This will run compilers for various OS and architecture combinations using docker.
Therefor docker must be running when building this project.
The `compileJni` task is integrated into the standard tasks, e.g. running `assemble` will
automatically rebuild the native library if required.

## Development

For the best development experience we recommend opening the `src/main/jni` directory
in Visual Studio Code with the CPP extension installed. The folder is configured to automatically
pick up the jni / jvmti header files from your `$JAVA_HOME` in order to provide autocompletion and
a pleasant development experience.
