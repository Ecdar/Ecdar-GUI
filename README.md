# Ecdar

Ecdar is an abbreviation of Environment for Compositional Design and Analysis of Real Time Systems.
This repo contains the source code for the graphical user interface, in order to run queries you will need the
j-ecdar and revaal executables.

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


