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
    private GameDescription gameDescription;

    /**
     * GameAnalyzer object which adds some tools for extracting information from the gameDescription object.
     */
    private GameAnalyzer gameAnalyzer;

    /**
     * The level we are generating.
     */
    private Level level;

    /**
     * Count down until level generation is due.
     */
    private ElapsedCpuTimer elapsedCpuTimer;

    /**
     * Random number generator.
     */
    private Random rng = new Random();

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
        level = new Level(10, 10, gameDescription.getLevelMapping());
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
        level.setSprite(3, 3, gameAnalyzer.getAvatarSprites().get(0));

        // Temporary, to prevent the game from finishing immediately
        level.setSprite(6, 6, gameAnalyzer.getGoalSprites().get(0));
    }

    /**
     * Fills every space that has not been assigned a spite yet, with a floor sprite if there exists such a sprite.
     */
    private void fillEmptySpaceWithFloor() {
        gameDescription.getStatic().stream().filter(spriteData -> spriteData.type.equals("Immovable")
                && !spriteData.isSingleton && !spriteData.isResource && !spriteData.isNPC && !spriteData.isAvatar
                && !spriteData.isPortal && gameAnalyzer.getOtherSprites().contains(spriteData.name)
        ).findFirst().ifPresent(spriteData -> level.setSpriteInEmptySpaces(spriteData.name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        initializeLevel();
        addSolidBorder();
        addPlayerAvatar();
        fillEmptySpaceWithFloor();
        return level.getLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashMap<Character, ArrayList<String>> getLevelMapping() {
        return level.getLevelMapping();
    }
}