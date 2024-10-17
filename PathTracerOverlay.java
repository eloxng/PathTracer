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
        // Get the client WorldView, the user's current tile and destination tile (WorldPoint)
        WorldView worldView = plugin.getWorldView();
        WorldPoint start = plugin.getWPCurrentTile();
        WorldPoint end = plugin.getWPDestinationTile();
        // Ensure valid start and end points
        if (start == null || end == null)
            return null; // Return if points are invalid
        // Get local points from WorldPoints
        LocalPoint localPoint1 = LocalPoint.fromWorld(worldView, start);
        LocalPoint localPoint2 = LocalPoint.fromWorld(worldView, end);
        if(localPoint1 != null && localPoint2 != null){
            // Get the tile polygon from the local points
            Polygon tilePoly1 = Perspective.getCanvasTilePoly(client, localPoint1);
            Polygon tilePoly2 = Perspective.getCanvasTilePoly(client, localPoint2);
            if(tilePoly1 != null && tilePoly2 != null){
                // Color the tile polygons
                OverlayUtil.renderPolygon(graphics, tilePoly1, config.setPathColor());
                OverlayUtil.renderPolygon(graphics, tilePoly2, Color.PINK);
            }
        }
        return null;
    }
}
