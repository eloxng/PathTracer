package net.runelite.client.plugins.pathtracer;

import net.runelite.api.*;

import net.runelite.api.Menu;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.lang.String;
import com.google.inject.Provides;



@PluginDescriptor(
        name = "Path Tracer",
        description = "Trace the shortest walking path to your destination",
        tags = {"pathfinder", "maps"}
)
public class PathTracerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PathTracerOverlay pathTracerOverlay;

    @Provides
    PathTracerConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(PathTracerConfig.class); }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(pathTracerOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(pathTracerOverlay);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event){
        // If you're right-clicking an in-game tile...
        if (event.getOption().equals("Walk here")) {
            addMenuEntries();
        }
    }
    private void addMenuEntries(){
        // Make a new menu using the client
        Menu menu = client.getMenu();
        // Create menu entries using the new Menu variable
        MenuEntry[] entries = menu.getMenuEntries();
        MenuEntry[] newEntries = new MenuEntry[entries.length + 2];
        System.arraycopy(entries, 0, newEntries, 0, entries.length);
        /*
        * When entry1 is selected
        * => get current and destination wp locations
        * => path find using the a* algorithm based on curr_wp and dest_wp
        * => Trigger the drawing of the path using pathTracerOverlay.render()
        * */
        MenuEntry entry1 = menu.createMenuEntry(entries.length)
                .setOption("Select destination")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> findPath());
        MenuEntry entry2 = menu.createMenuEntry(entries.length)
                .setOption("Clear the path")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> clearPath());
        // Set the menu entries into 'menu'
        newEntries[newEntries.length - 2] = entry2;
        newEntries[newEntries.length - 1] = entry1;
        menu.setMenuEntries(newEntries);
    }

    // Returns destination tile as a LocalPoint
    public LocalPoint getLPDestinationTile() {
        WorldView wv = client.getTopLevelWorldView();
        Tile target = wv.getSelectedSceneTile();
        if(target == null)
            return null;
        return target.getLocalLocation();
    }
    // Returns the current tile as a LocalPoint
    public LocalPoint getLPCurrentTile() {
        Player localPlayer = client.getLocalPlayer();
        if(localPlayer == null)
            return null;
        return localPlayer.getLocalLocation();
    }

    // Returns the destination tile as a WorldPoint
    public WorldPoint getWPDestinationTile(){ return WorldPoint.fromLocalInstance(client, getLPDestinationTile()); }
    // Returns the current tile as a WorldPoint
    public WorldPoint getWPCurrentTile(){ return WorldPoint.fromLocalInstance(client, getLPCurrentTile()); }
    /*  -- To extract data from WorldPoint --
        int regionId = worldPoint.getRegionID(),
                regionX = worldPoint.getRegionX(),
                regionY = worldPoint.getRegionY(),
                plane = getWorldView().getPlane();
    */
    // Returns a WorldView
    public WorldView getWorldView(){
        return client.getTopLevelWorldView();
    }

    private void findPath() {
        // Invoke drawTiles from pathTracerOverlay to render the tiles
        pathTracerOverlay.drawTiles(getWorldView(), getWPCurrentTile(), getWPDestinationTile());
    }
    // Pass null for all three parameters in drawTiles()
    private void clearPath() { pathTracerOverlay.drawTiles(null, null, null); }

    /* Functions for testing */
    private void printTileCoord(String type, int regionId, int regionX, int regionY, int plane) {
        String msg =  type + ": (" +
                "rid: " + regionId +
                ", rx: " + regionX +
                ", ry: " + regionY +
                ", z: " + plane + ")";
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
    }
}

