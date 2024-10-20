package net.runelite.client.plugins.pathtracer;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;


public class PathTracerOverlay extends Overlay {
    private final Client client;
    private final PathTracerPlugin plugin;
    private final PathTracerConfig config;
    private TileNode path;


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
        TileNode current = path;
        while(current != null){
            if(current.getLocalPoint() != null){
                Polygon poly = Perspective.getCanvasTilePoly(client, current.getLocalPoint());
                if(poly != null)
                    OverlayUtil.renderPolygon(graphics, poly, config.setCurrColor());
            }
            current = current.getParent();
        }
        return null;
    }

    // Add the path list from plugins
    public void addTilesToDraw(TileNode path){ this.path = path; }
}
