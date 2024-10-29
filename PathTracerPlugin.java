package net.runelite.client.plugins.pathtracer;

import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
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
    private ClientThread clientThread;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PathTracerOverlay pathTracerOverlay;

    // Offsets used for getNeighbors()
    // ORDER: SW W NW S N SE E NE
    private final int[][] offsets = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},         {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    
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
    public WorldView getWorldView(){ return client.getTopLevelWorldView(); }

    // Pass null for all three parameters in drawTiles()
    private void clearPath() {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Clearing path", "");
        pathTracerOverlay.addTilesToDraw(null);
    }

    // Function that gets executed when "Select destination" is pressed
    private void findPath() {
        // Initialize start and goal LocalPoints
        LocalPoint start = getLPCurrentTile();
        LocalPoint goal = getLPDestinationTile();
        // If start/goal is somehow null
        if (start == null || goal == null) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Start and or goal point is null.", "");
            return;
        }
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Start and or goal point initialized.", "");

        // Initialize startTile and goalTile TileNode's
        TileNode startTile = new TileNode(start);
        TileNode goalTile = new TileNode(goal);
        startTile.setGcost(0);
        startTile.setHcost(calculateManhattanDistance(startTile, goalTile));
        startTile.setFcost(startTile.getGcost() + startTile.getHcost());
        goalTile.setHcost(0);

        // Draw path
        TileNode path = AStarPath(startTile, goalTile);
        if (path != null)
            pathTracerOverlay.addTilesToDraw(path);
    }

    private TileNode AStarPath(TileNode start, TileNode goal){
        // Initialize open and closed lists
        PriorityQueue<TileNode> openList = new PriorityQueue<>(Comparator.comparingInt(t -> t.getFcost()));
        Set<TileNode> closedList = new HashSet<>();
        openList.add(start);

        while(!openList.isEmpty()) {
            // Get node with lowest fCost
            TileNode currentTile = openList.poll();
            closedList.add(currentTile);

            // If current tile reaches the destination
            if(currentTile.getLocalPoint().equals(goal.getLocalPoint())){
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Path found", "");
                return currentTile;
            }

            // Check the neighboring tiles
            for(TileNode neighbor : getNeighbors(currentTile)){
                if(closedList.contains(neighbor))
                    continue;

                // Calculate tentative g cost
                int tgcost = currentTile.getGcost() + calculateManhattanDistance(currentTile, neighbor);

                if(tgcost < neighbor.getGcost() || !openList.contains(neighbor)) {
                    // Set parent
                    neighbor.setParent(currentTile);
                    // Set the g, h, and f cost of the neighbor
                    neighbor.setGcost(tgcost);
                    neighbor.setHcost(calculateManhattanDistance(neighbor,goal));
                    neighbor.setFcost(neighbor.getGcost() + neighbor.getHcost());

                    // Add neighbor if openList doesn't have it already
                    if(!openList.contains(neighbor))
                        openList.add(neighbor);
                }
            }

        }
        return null;
    }

    // Return a List<TileNode> that has all the current TileNode's neighbors
    private List<TileNode> getNeighbors(TileNode current) {
        List<TileNode> neighbors = new ArrayList<>();
        int sceneX = current.getLocalPoint().getSceneX();
        int sceneY = current.getLocalPoint().getSceneY();

        // Offsets for the 8 possible neighbors
        for(int[] offset: offsets){
            // Get neighboring scene x and y [coordinates
            int nSceneX = sceneX + offset[0];
            int nSceneY = sceneY + offset[1];

            // Skip if this neighbor is out of bounds or blocked by a wall
            LocalPoint neighborPoint = LocalPoint.fromScene(nSceneX, nSceneY, getWorldView());
            if(canMoveTo(current.getLocalPoint(), neighborPoint))
                neighbors.add(new TileNode(neighborPoint));
        }
        return neighbors;
    }

    // Helper function to findPath() and AStarPath()
    // Determines distance from one TileNode to another TileNode
    // Used as the h cost for a TileNode
    private int calculateManhattanDistance(TileNode start, TileNode goal) {

        /*
            => API built in distance calculation: start.getLocalPoint().distanceTo(goal.getLocalPoint());
            => Manhattan Distance: Math.abs(start.getLocalPoint().getSceneX() - goal.getLocalPoint().getSceneX()) +
                Math.abs(start.getLocalPoint().getSceneY() - goal.getLocalPoint().getSceneY());
        */
        return Math.abs(start.getLocalPoint().getSceneX() - goal.getLocalPoint().getSceneX()) +
                Math.abs(start.getLocalPoint().getSceneY() - goal.getLocalPoint().getSceneY());
    }

    private boolean canMoveTo(LocalPoint currentPoint, LocalPoint neighborPoint) {
        int currentX = currentPoint.getSceneX();
        int currentY = currentPoint.getSceneY();
        int neighborX = neighborPoint.getSceneX();
        int neighborY = neighborPoint.getSceneY();

        CollisionData[] collisionData = getWorldView().getCollisionMaps();
        if (collisionData == null || collisionData.length == 0 || collisionData[getWorldView().getPlane()] == null)
            return false; // Assume there's a wall if no collision data

        // Get collision data
        int[][] colData = collisionData[getWorldView().getPlane()].getFlags();

        // First, check if the target tile itself is walkable
        if ((colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
            return false;

        // Determine movement direction based on the relative position of neighbor to current
        int deltaX = neighborX - currentX;
        int deltaY = neighborY - currentY;
        if (deltaX == 0 && deltaY == 1) {
            // Moving North
            return (colData[currentX][currentY] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 &&
                    (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
        } else if (deltaX == 0 && deltaY == -1) {
            // Moving South
            return (colData[currentX][currentY] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 &&
                    (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        } else if (deltaX == 1 && deltaY == 0) {
            // Moving East
            return (colData[currentX][currentY] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0 &&
                    (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
        } else if (deltaX == -1 && deltaY == 0) {
            // Moving West
            return (colData[currentX][currentY] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0 &&
                    (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
        } else {
            // Not a valid movement direction (e.g., diagonal not supported or points are the same)
            return false;
        }
    }
}

