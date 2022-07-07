# Ecdar
Ecdar stands for Environment for Compositional Design and Analysis of Real Time Systems.

A list of publications involving Ecdar can be found at [http://ulrik.blog.aau.dk/ecdar/](http://ulrik.blog.aau.dk/ecdar/)

## How to run
Executables for both UNIX-based systems and Windows are located in the `bin` directory. You will need a version of Java 11 with JavaFX. The application has been tested with the Azul Zulu 11 JavaFX JDK, which can be downloaded here: [https://www.azul.com/downloads/?version=java-11-lts&package=jdk-fx](https://www.azul.com/downloads/?version=java-11-lts&package=jdk-fx). Make sure that your system is using the correct Java version when trying to run the application.

### Getting started
To see what the system is capable of, open one of the example projects by clicking `File > Open project` and then selecting one of the examples in the `examples` directory. `EcdarUniversity` is suggested as a good starting point.

### Setting up engines
This application is bundled with the [Reveaal](https://github.com/Ecdar/Reveaal) engine, which can be loaded into the application by opening the `Backend options` dialog under the `Options` tab in the top menu and then pressing the `Reset backends` button in the top-right corner. This will be done automatically when the application is opened for the first time.

The `Backend options` dialog allows you to add additional engines by pressing the `+` symbol at the bottom. This will create a new engine instance, which allows you to target an engine in one of two ways:
- **Local program**: If you have a local instance of an engine, you can check the `local` checkbox and specify a path to the engine. The application will take care of starting and terminating instances of the engine, using the specified port range. NOTE: Make sure that the user executing the application has permission to execute the engine program.
- **Remote process**: If you have access to a server running an engine, you can uncheck the `local` checkbox and specify an address and port range at which the engine can be reached.

Whether you are using a local or a remote engine, make sure that it supports the Ecdar ProtoBuf communication standard, which can be found here: [https://github.com/Ecdar/Ecdar-ProtoBuf](https://github.com/Ecdar/Ecdar-ProtoBuf).