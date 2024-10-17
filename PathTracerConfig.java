package net.runelite.client.plugins.pathtracer;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PathTracerConfig.GROUP)
public interface PathTracerConfig extends Config {
    String GROUP = "pathtracer";
    // Set color of the path
    @ConfigItem(
            keyName = "setPathColor",
            name = "Set path color",
            description = "Set the color of the tiles that makes the path",
            position = 1
    )
    default Color setPathColor() {return Color.blue;}

    @ConfigItem(
            keyName = "setCurrColor",
            name = "Current Location",
            description = "Color of the current location of the runescaper",
            position = 2
    )
    default Color setCurrColor() {return Color.blue;}

    @ConfigItem(
            keyName = "setDestColor",
            name = "Destination",
            description = "Color of the selected destination of the runescaper",
            position = 3
    )
    default Color setDestColor() {return Color.blue;}
}
