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
import java.lang.String;
import java.util.*;

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
        if (event.getOption().equals("Walk here"))
            addMenuEntries();
    }
    private void addMenuEntries(){
        // Make a new menu using the client
        Menu menu = client.getMenu();
        // Create menu entries using the new Menu variable
        MenuEntry[] entries = menu.getMenuEntries();
        MenuEntry[] newEntries = new MenuEntry[entries.length + 2];
        System.arraycopy(entries, 0, newEntries, 0, entries.length);
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
        Tile target = client.getTopLevelWorldView().getSelectedSceneTile();
        if(target == null)
            return null;
        return target.getLocalLocation();
    }
    // Returns the current tile as a LocalPoint
    public LocalPoint getLPCurrentTile() {
        if(client.getLocalPlayer() == null)
            return null;
        return client.getLocalPlayer().getLocalLocation();
    }
    // Returns the destination tile as a WorldPoint
    public WorldPoint getWPDestinationTile(){ return WorldPoint.fromLocalInstance(client, getLPDestinationTile()); }
    // Returns the current tile as a WorldPoint
    public WorldPoint getWPCurrentTile(){ return WorldPoint.fromLocalInstance(client, getLPCurrentTile()); }
    // Returns a WorldView
    public WorldView getWorldView(){
        return client.getTopLevelWorldView();
    }
    // Pass null for all three parameters in drawTiles()
    private void clearPath() { pathTracerOverlay.addTilesToDraw(null); }

    // Function that gets executed when "Select destination" is pressed
    private void findPath() {
        // Initialize variables needed for AStarPath(TileNode,TileNode)
        LocalPoint start = getLPCurrentTile();
        LocalPoint goal = getLPDestinationTile();
        TileNode startTile = new TileNode(start);
        TileNode goalTile = new TileNode(goal);
        if (start == null || goal == null) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Start or goal point is null.", "");
            return;
        }
        // Initialize startTile and goalTile f g and h costs
        startTile.setCosts(0, calculateManhattanDistance(startTile, goalTile), startTile.getGcost() + startTile.getHcost());
        goalTile.setHcost(0);
        // Send path to pathTracerOverlay
        pathTracerOverlay.addTilesToDraw(AStarPath(startTile, goalTile));
    }

    public List<TileNode> AStarPath(TileNode start, TileNode goal){
        // Initialize open and closed lists
        List<TileNode> openList = new ArrayList<>();
        List<TileNode> closedList = new ArrayList<>();
        openList.add(start);

        while(!openList.isEmpty()) {
            // Get node with lowest fCost
            TileNode currentTile = getLowestFCostNode(openList);

            // If current tile reaches the destination
            if(currentTile.equals(goal))
                return constructPath(currentTile);

            // Move current tile to closed list
            openList.remove(currentTile);
            closedList.add(currentTile);

            // Check the neighboring tiles
            for(TileNode neighbor : getNeighbors(currentTile)){
                if(closedList.contains(neighbor) || !isTileWalkable(neighbor.getLocalPoint()))
                    continue; // Skip neighbor that is closed or not walkable

                // Calculate tentative g cost
                int ct_gcost = currentTile.getGcost(); // Get current tile's gcost
                int distbtw_ct_gt = calculateManhattanDistance(currentTile, neighbor); // Distance between current tile AND neighbortile
                int tgcost = ct_gcost + distbtw_ct_gt; // tentativegcost

                // Add neighbor if openList doesn't have it already
                if(!openList.contains(neighbor))
                    openList.add(neighbor);
                else if (tgcost >= neighbor.getGcost())
                    continue;

                neighbor.setParent(currentTile); // Set parent
                int hcost = calculateManhattanDistance(neighbor,goal);
                int fcost = neighbor.getGcost() + neighbor.getHcost();
                neighbor.setCosts(tgcost, hcost, fcost);
            }
        }
        // Return an empty list if no path is found
        return new ArrayList<>();
    }

    // Return TileNode with the lowest fcost from List<TileNode>
    private TileNode getLowestFCostNode(List<TileNode> openList) {
        TileNode desiredNode = null;
        for(TileNode tile : openList){
            if (desiredNode == null || tile.getFcost() < desiredNode.getFcost()) {
                desiredNode = tile;
            }
        }
        return desiredNode;
    }

    // Return a List<TileNode> that has all the current TileNode's neighbors
    private List<TileNode> getNeighbors(TileNode current) {
        List<TileNode> neighbors = new ArrayList<>();
        // Get the scene x and y's of the current
        int sceneX = current.getLocalPoint().getSceneX();
        int sceneY = current.getLocalPoint().getSceneX();

        // Offsets for the 8 possible neighbors
        int[][] offsets = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},         {0, 1},
                {1, -1}, {1, 0}, {1, 1}
        };

        for(int[] offset: offsets){
            // Get neighboring scene x and y coordinates
            int nSceneX = sceneX + offset[0];
            int nSceneY = sceneY + offset[1];
            // Make a new local point for the neighboring point
            LocalPoint neighborPoint = LocalPoint.fromScene(nSceneX, nSceneY, getWorldView());
            // Add the neighboring point to neighbors list if that point is walkable
            if(isTileWalkable(neighborPoint))
                neighbors.add(new TileNode(neighborPoint));
        }
        return neighbors;
    }

    // Returns the made path
    private List<TileNode> constructPath(TileNode tile){
        List<TileNode> path = new ArrayList<>();
        while(tile != null){
            path.add(tile);
            tile = tile.parent;
        }
        Collections.reverse(path);
        return path;
    }

    // Heuristic to calculate distance between nodes
    private int calculateManhattanDistance(TileNode start, TileNode goal) {
        return Math.abs(start.getLocalPoint().getSceneX() - goal.getLocalPoint().getSceneX()) +
                Math.abs(start.getLocalPoint().getSceneY() - goal.getLocalPoint().getSceneY());
    }

    // Determines if an in-game tile is walkable based off the collision map rendered from the user's WorldView
    private boolean isTileWalkable(LocalPoint tile){
        int colFlag;
        CollisionData[] collisionData = getWorldView().getCollisionMaps();
        int sceneX = tile.getSceneX();
        int sceneY = tile.getSceneY();
        if (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104) {
            int[][] colData = collisionData[0].getFlags();
            colFlag = colData[sceneX][sceneY];
            // Return based on if colFlag of tile matches the BLOCK_MOVEMENT_FULL flag from CollisionDataFlag[]
            return (colFlag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        return false; // If not in scene, then not walkable
    }
}

