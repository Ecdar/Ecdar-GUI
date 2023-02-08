# Ecdar
Ecdar is an abbreviation of Environment for Compositional Design and Analysis of Real Time Systems.
This repo contains the source code for the graphical user interface. In order to run queries you will need the
j-ecdar and revaal executables.

> :information_source: If the goal is to use ECDAR, please goto the [main ECDAR repository](https://github.com/Ecdar/ECDAR), which contains releases for all supported platforms. These releases contain all dependencies, including the engines and a JRE.

<a id="dependencies"></a>
## Dependencies
This section covers what dependencies are currently needed by the GUI.

### JVM
As with all Java applications, a working JVM is required to run the project. 

You will need Java version 11 containing JavaFX. We suggest downloading from https://www.azul.com/downloads/?version=java-11-lts&package=jdk-fx, as this is the version used by the main development team.

### Ecdar-ProtoBuf
This repository utilizes the [Ecdar-ProtoBuf repository](https://github.com/Ecdar/Ecdar-ProtoBuf) for the communication with the engines. This dependency is implemented as a submodule that needs to be pulled and updated. If you have not yet cloned the code from this repository (the GUI), you can clone both the GUI and the submodule containing the ProtoBuf repository by running the following command:

``` sh
git clone --recurse-submodules git@github.com:Ecdar/Ecdar-GUI.git
```

If you have already cloned this repository, you can clone the ProtoBuf submodule by running the following command from a terminal in the GUI repository directory:

``` sh
git submodule update --init --recursive
```

### Engines (needed for model-checking)
In order to use the model-checking capabilities of the system, it is necessary to download at least one engine for the used operating system and place it in the `lib` directory.

> :information_source: The latest version of each engine can be downloaded from:
> * https://github.com/Ecdar/j-Ecdar
> * https://github.com/Ecdar/Reveaal

The engines can then be configured in the GUI as described in [Engine Configuration](#engine_configuration).

## How to Run
After having retrieved the code and acquired all the dependencies mentioned in [Dependencies](#dependencies), the GUI can be started using the following command:
``` sh
./gradlew(.bat) run #Depending on OS
```

<a id="engine_configuration"></a>
## Engine Configuration
In order to utilize the model-checking capabilities of the system, at least one engine must be configured. The distributions available at [ECDAR](https://github.com/Ecdar/ECDAR) will automatically load the default engines on startup, but this is currently not working when running the GUI through Gradle. For the same reason, the `Reset Engines` button will clear the engines but will not be able to load the packaged once.

An engine can be added through the configurator found under `Options > Engines Options` in the menubar, which opens the pop-up shown below.

<img src="presentation/EngineConfiguration.png" alt="Engine Configuration Pop-up">

> :information_source: If you accidentally removed or changed an engine, these changes can be reverted be pressing `Cancel` or by clicking outside the pop-up. Consequently, if any changes should be saved, **MAKE SURE TO PRESS `Save`** 

### Address
The _Address_ is either the address of a server running the engine (for remote execution) or a path to a local engine binary (for this, the _Local_ checkbox must be checked).

### Port range
The GUI uses gRPC for the communication with the engines and will therefore need at least one free port. This range directly limits the number of instances of the engine that will be started.
> :warning: Make sure AT LEAST one port is free within the specified range. For instance, the default port range for Reveaal is _5032_ - _5040_.

### Default
If an engine is marked with _Default_, all added queries will be assigned that engine.

## Screenshots

| <img src="presentation/Retailer.png" width="400">  <img src="presentation/Administration.png" width="400"> | <img src="presentation/UniversityExample.png" width="400"> | 
|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|

## Exemplary Projects
To get started and get an idea of what the system can be used for, multiple exemplary can be found in the `examples` directory.

## H-UPPAAL
This project is a hard fork of https://github.com/ulriknyman/H-Uppaal.


