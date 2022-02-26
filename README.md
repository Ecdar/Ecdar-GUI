# Ecdar

Ecdar is an abbreviation of Environment for Compositional Design and Analysis of Real Time Systems.
This repo contains the source code for the graphical user interface, in order to run queries you will need the
j-ecdar and revaal executables.

## Dependencies
This repository utilizes the Ecdar-Proto repository for structuring the communication between the GUI and the engines. This dependency is implemented as a submodule which needs to be pull and updated. If you have not yet cloned the code in this repository (the GUI), you can clone both the GUI and the Proto repository by running the command below:

``` sh
git clone --recurse-submodules git@github.com:Ecdar/Ecdar-GUI.git
```

If you have already cloned this repository, you can clone the Proto submodule by running the command below, from a terminal inside the cloned GUI repository:

``` sh
git submodule update --init --recursive
```

## How to Run
You will need a working JVM verion 11 with java FX in order to run the GUI. We suggest downloading from https://www.azul.com/downloads/?version=java-11-lts&package=jdk-fx.

To run the gui use the gradle wrapper script 

``` sh
./gradlew(.bat) run 
```

## Engine Configuration
Download the latest version of the engine from: 

  * https://github.com/Ecdar/j-Ecdar
  * https://github.com/Ecdar/Reveaal

Unpack and move the downloaded files to the `lib` folder. You can also configure custom engine locations from the GUI. 


## Screenshots

| <img src="presentation/Retailer.png" width="400">  <img src="presentation/Administration.png" width="400"> | <img src="presentation/UniversityExample.png" width="400"> | 
|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|

Sample Projects
----
See sample projects in the `samples` folder.


H-UPPAAL
----------
This project is a hard fork of https://github.com/ulriknyman/H-Uppaal.


