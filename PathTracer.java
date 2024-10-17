package net.runelite.client.plugins.pathtracer;

import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

/*
* Based on the A* algorithm
* */

public class PathTracer {
    WorldPoint worldPoint;
    PathTracer cameFrom;
    int gCost;
    int hCost;
    int fCost;

    PathTracer(WorldPoint worldPoint, PathTracer cameFrom, int gCost, int hCost)
    {
        this.worldPoint = worldPoint;
        this.cameFrom = cameFrom;
        this.gCost = gCost;
        this.hCost = hCost;
        this.fCost = gCost + hCost;
    }
}
