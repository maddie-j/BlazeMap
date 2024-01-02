![Blaze Map Logo](https://raw.githubusercontent.com/LordFokas/BlazeMap/master/images/BlazeMap_Logo.png)

The open source mapping mod for Forge

## Dev Instructions

### Dependencies

Java: Make sure Java 17 is installed and is the version listed in `JAVA_HOME`.

Rubidium: Place a copy of the Rubidium jar matching the version listed in `build.gradle.kts`
into the `libs` folder.

### Build

This project is built using Gradle.

To run the build commands, you can either use the Gradle helper built into your IDE, or you
can use `gradlew` (Windows) or `./gradlew` (Linux/Mac) on the command line.

* `gradlew build`: Build the app.
* `gradlew runClient`: Start a local dev server in single player mode.
* `gradlew runServer`: Start a local dev server in server mode only, which can be connected to
  from the client.
* `gradlew jar`: Bundle the mod into a jar file that can be distributed. Output will be in `build/libs`.
* `gradlew tasks`: See all available Gradle commands.

#### WSL

Note: If you're developing in WSL2, you may have issues running `runClient` and `runServer`.
Installing the official Minecraft Launcher and through it the correct version of Minecraft
does _not_ seem to fix this issue.

However, you should still be able to build a jar file and copy that into your Minecraft mods
folder on your Windows desktop userspace.

<!--
To install Minecraft within WSL, you will need to install the launcher within your WSL2 instance
and use it to install the version of Minecraft you're using:

1. Go to https://www.minecraft.net/en-us/download and copy the link to the correct package.
2. Use `wget` to download the package: `wget <link you copied>`.
3. Install the package using `sudo dpkg -i Minecraft.deb`.
4. Install the dependencies using `sudo apt -f install`.
5. Start the launcher using `minecraft-launcher` and login + install Minecraft 1.18.2 from there.

The local dev server commands should run after that. -->
