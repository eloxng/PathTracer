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
            description = "Set the color of the tiles that make the path",
            position = 1
    )
    default Color setPathColor() {return Color.blue;}

    @ConfigItem(
            keyName = "borderWidth",
            name = "Border Width",
            description = "Width of the marked tile border",
            position = 2
    )
    default double borderWidth()
    {
        return 2;
    }
}
