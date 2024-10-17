package net.runelite.client.plugins.pathtracer;

import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
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
        MenuEntry[] newEntries = new MenuEntry[entries.length + 3];
        System.arraycopy(entries, 0, newEntries, 0, entries.length);
        MenuEntry entry1 = menu.createMenuEntry(entries.length)
                .setOption("Select destination")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> getWPDestinationTile());
        MenuEntry entry2 = menu.createMenuEntry(entries.length)
                .setOption("Get current location")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> getWPCurrentTile());
        MenuEntry entry3 = menu.createMenuEntry(entries.length)
                .setOption("Clear the path")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Removing the traced path...", null);
                });
        // Set the menu entries into 'menu'
        newEntries[newEntries.length - 3] = entry3;
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
        WorldView wv = client.getTopLevelWorldView();
        Player localPlayer = client.getLocalPlayer();
        if(localPlayer == null)
            return null;
        return localPlayer.getLocalLocation();
    }

    // Returns the current tile as a WorldPoint
    public WorldPoint getWPDestinationTile(){
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, getLPDestinationTile());
        /*
        int regionId = worldPoint.getRegionID(),
                regionX = worldPoint.getRegionX(),
                regionY = worldPoint.getRegionY(),
                plane = getWorldView().getPlane();
        // Print message into chat box
        printTileCoord("Current tile", regionId, regionX, regionY, plane);
        */
        return worldPoint;
    }

    // Returns the current tile as a WorldPoint
    public WorldPoint getWPCurrentTile(){
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, getLPCurrentTile());
        /*
        int regionId = worldPoint.getRegionID(),
                regionX = worldPoint.getRegionX(),
                regionY = worldPoint.getRegionY(),
                plane = getWorldView().getPlane();
        // Print message into chat box
        printTileCoord("Current tile", regionId, regionX, regionY, plane);
        */
        return worldPoint;
    }
    // Returns a WorldView
    public WorldView getWorldView(){
        return client.getTopLevelWorldView();
    }

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

