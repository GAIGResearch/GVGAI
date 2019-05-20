package username;

import core.game.GameDescription;
import core.generator.AbstractLevelGenerator;
import tools.ElapsedCpuTimer;
import tracks.levelGeneration.constructiveLevelGenerator.LevelGenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class Agent extends AbstractLevelGenerator {

    /**
     * Placeholder
     */
    private AbstractLevelGenerator generator;

    /**
     * Placeholder
     */
    public Agent(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        generator = new LevelGenerator(game, elapsedTimer);
    }

    /**
     * Placeholder
     */
    @Override
    public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        return generator.generateLevel(game, elapsedTimer);
    }

    /**
     * Placeholder
     */
    @Override
    public HashMap<Character, ArrayList<String>> getLevelMapping() {
        return generator.getLevelMapping();
    }
}