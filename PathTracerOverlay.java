package net.runelite.client.plugins.pathtracer;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.api.CollisionData;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

public class PathTracerOverlay extends Overlay {
    private final Client client;
    private final PathTracerPlugin plugin;
    private final PathTracerConfig config;
    private List<TileNode> path;

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
        if(path == null || path.isEmpty())
            return null;

        for(TileNode tile : path){
            Polygon poly = Perspective.getCanvasTilePoly(client, tile.getLocalPoint());
            if(poly != null){
                OverlayUtil.renderPolygon(graphics, poly, config.setPathColor());
            }
        }
        return null;
    }

    // Add the path list from plugins
    public void addTilesToDraw(List<TileNode> path){
        if(path == null)
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Path passed as null, clearing path...", "");
        else
            this.path = path;
    }
}
