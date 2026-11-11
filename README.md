# Galaxy Pearl (Fabric, Minecraft 1.21.11, client-side)

When your ender pearl lands, a spinning spiral galaxy + starfield fades in over the screen,
then fades out (about 1.8 s) while you pop out at the new spot.

## Build
1. Install Java 21 and open this folder in IntelliJ IDEA (or run `gradle wrapper` once, then `./gradlew build`).
2. Output jar: `build/libs/galaxy-pearl-1.0.0.jar` (not the `-sources` jar).
3. Put it in `.minecraft/mods` together with Fabric Loader 0.18.1+ and Fabric API for 1.21.11.

If Gradle says a version doesn't exist, update gradle.properties from https://fabricmc.net/develop/ .

## Commands
- `/galaxypearl test` plays the animation without throwing a pearl
- `/galaxypearl enabled true|false` turns it on/off

## Tweaks (GalaxyOverlay.java)
`DURATION`, `FADE_IN`, `FADE_OUT`, `STARS`, `GALAXY_POINTS`, colors in `init()`.
