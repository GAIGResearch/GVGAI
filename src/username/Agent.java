package username;

import core.game.GameDescription;
import core.generator.AbstractLevelGenerator;
import tools.ElapsedCpuTimer;
import tools.GameAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Agent that generates a level from a GameDescription.
 */
public class Agent extends AbstractLevelGenerator {

    /**
     * GameDescription object holding all game information.
     */
    private final GameDescription gameDescription;

    /**
     * GameAnalyzer object which adds some tools for extracting information from the gameDescription object.
     */
    private final GameAnalyzer gameAnalyzer;

    /**
     * The level we are generating.
     */
    private Level level;

    /**
     * Count down until level generation is due.
     */
    private final ElapsedCpuTimer elapsedCpuTimer;

    /**
     * Random number generator.
     */
    private final Random rng = new Random();

    /**
     * The x position of the player avatar.
     */
    private int avatarX;

    /**
     * The y position of the player avatar.
     */
    private int avatarY;

    /**
     * Constructor for the level generator.
     * @param game GameDescription object holding all game information.
     * @param elapsedTimer Count down until level generation is due.
     */
    public Agent(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        gameDescription = game;
        gameAnalyzer = new GameAnalyzer(game);
        elapsedCpuTimer = elapsedTimer;
    }

    /**
     * Determines the width and height of the leven and initializes a new Level object.
     * @see Level
     */
    private void initializeLevel() {
        // TODO determine a good level size, depending on the game description
        level = new Level(10, 10);
    }

    /**
     * Adds a border of random solid sprites around the level.
     */
    private void addSolidBorder() {
        // TODO determine if this type of game should get a solid border
        List<String> solidSprites = gameAnalyzer.getSolidSprites();
        if (solidSprites.isEmpty()) return;
        String solidSprite = solidSprites.get(rng.nextInt(solidSprites.size()));

        // Add a border along the top and bottom side of the level
        for (int x = 0; x < level.getWidth(); x++) {
            level.setSprite(x, 0, solidSprite);
            level.setSprite(x, level.getHeight() - 1, solidSprite);
        }

        // Add a border along the left and right side of the level
        for (int y = 1; y < level.getHeight() - 1; y++) {
            level.setSprite(0, y, solidSprite);
            level.setSprite(level.getWidth() - 1, y, solidSprite);
        }
    }

    /**
     * Places the player avatar into the level.
     */
    private void addPlayerAvatar() {
        // TODO determine a good location to add the player avatar.
        avatarX = 3;
        avatarY = 3;

        level.addSprite(avatarX, avatarY, gameAnalyzer.getAvatarSprites().get(0));

        // Temporary, to prevent the game from finishing immediately
        level.addSprite(6, 6, gameAnalyzer.getGoalSprites().get(0));
    }

    /**
     * Fills every space that has not been assigned a spite yet, with a floor sprite if there exists such a sprite.
     */
    private void fillEmptySpaceWithFloor() {
        gameDescription.getStatic().stream().filter(spriteData -> "Immovable".equals(spriteData.type)
                && !spriteData.isSingleton && !spriteData.isResource && !spriteData.isNPC && !spriteData.isAvatar
                && !spriteData.isPortal && gameAnalyzer.getOtherSprites().contains(spriteData.name)
        ).findAny().ifPresent(spriteData -> {
            // If we can find a sprite the looks like a floor tile, add it to all coordinates
            String sprite = spriteData.name;
            level.forEachPosition((x, y) -> level.addSprite(x, y, sprite));
        });
    }

    /**
     * Fills unreachable areas of the level with solid sprites.
     */
    private void fillUnreachablePositions() {
        List<String> solidSprites = gameAnalyzer.getSolidSprites();
        if (solidSprites.isEmpty()) return;

        // Check which areas are reachable
        boolean[][] reachable = new boolean[level.getHeight()][level.getWidth()];
        findReachablePositions(reachable, avatarX, avatarY, solidSprites);

        // Fill unreachable areas with solid sprites
        String solidSprite = solidSprites.get(rng.nextInt(solidSprites.size()));
        level.forEachPosition((x, y) -> {
            if (!reachable[y][x]) level.setSprite(x, y, solidSprite);
        });
    }

    /**
     * Finds positions which are reachable from the given position.
     * @param reachable Matrix of booleans, indicating which positions are reachable, and which are not.
     * @param x The x position we are trying to reach other positions from.
     * @param y The y position we are trying to reach other positions from.
     * @param sprites The list of solid sprites.
     */
    private void findReachablePositions(boolean[][] reachable, int x, int y, List<String> sprites) {
        // If this coordinate has been checked before, do nothing
        if (reachable[y][x]) return;
        reachable[y][x] = true;

        // If this coordinate contains a solid sprite, then neighbouring sprites won't be reachable from here
        if (sprites.stream().anyMatch(level.get(x, y)::contains)) return;

        // Get coordinates
        int[] xCoordinates = {x - 1, x, x + 1, x};
        int[] yCoordinates = {y, y + 1, y, y - 1};

        for (int i = 0; i < xCoordinates.length; i++) {
            // Check x coordinate is within bounds
            int xi = xCoordinates[i];
            if (xi < 0 || xi >= level.getWidth()) continue;

            // Check y coordinate is within bounds
            int yi = yCoordinates[i];
            if (yi < 0 || yi >= level.getHeight()) continue;

            // Check the the coordinate
            findReachablePositions(reachable, xi, yi, sprites);
        }
    }

    @Override
    public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        initializeLevel();
        addSolidBorder();
        addPlayerAvatar();
        fillEmptySpaceWithFloor();
        fillUnreachablePositions();
        return level.getLevel();
    }

    @Override
    public HashMap<Character, ArrayList<String>> getLevelMapping() {
        return Level.getLEVEL_MAPPING().getCharMapping();
    }
}