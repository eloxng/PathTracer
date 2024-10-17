package net.runelite.client.plugins.pathtracer;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.api.CollisionData;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;

public class PathTracerOverlay extends Overlay {
    private final Client client;
    private final PathTracerPlugin plugin;
    private final PathTracerConfig config;

    private WorldView worldView;
    private WorldPoint start;
    private WorldPoint end;

    @Inject
    private PathTracerOverlay(Client client, PathTracerPlugin plugin, PathTracerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics){
        // Ensure valid start and end points
        if (getStart() == null || getEnd() == null)
            return null; // Return if points are invalid
        // Get local points from WorldPoints
        LocalPoint localPoint1 = LocalPoint.fromWorld(getWV(), getStart());
        LocalPoint localPoint2 = LocalPoint.fromWorld(getWV(), getEnd());
        if(localPoint1 != null && localPoint2 != null){
            // Get the tile polygon from the local points
            Polygon tilePoly1 = Perspective.getCanvasTilePoly(client, localPoint1);
            Polygon tilePoly2 = Perspective.getCanvasTilePoly(client, localPoint2);
            if(tilePoly1 != null && tilePoly2 != null){
                // Color the tile polygons
                OverlayUtil.renderPolygon(graphics, tilePoly1, config.setCurrColor());
                OverlayUtil.renderPolygon(graphics, tilePoly2, config.setDestColor());
            }
        }
        return null;
    }

    // Gets the parameters from PathTracerPlugin and set's them in here for render() to use
    public void drawTiles(WorldView worldView, WorldPoint start, WorldPoint end){
        this.worldView = worldView;
        this.start = start;
        this.end = end;
    }
    // Get's for render() to use
    private WorldView getWV() { return worldView; }
    private WorldPoint getStart() { return start; }
    private WorldPoint getEnd() { return end; }
}
