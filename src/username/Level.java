package username;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing a matrix of sets, containing strings which represent sprites, representing a level.
 */
@SuppressWarnings("PMD.LooseCoupling")
public class Level {

    /**
     * The level we are generating, represented as a matrix of sets, containing strings which represent sprites.
     * The outer list contains the rows, i.e. y coordinate.
     * The middle lists contain sprite lists, i.e. x coordinate.
     * The inner lists are sprite lists.
     */
    private final List<List<ArrayList<String>>> levelMatrix;

    /**
     * The width of the levelMatrix.
     */
    @Getter private int width;

    /**
     * The height of the levelMatrix.
     */
    @Getter private int height;

    /**
     * The levelMapping for this level. Only gets generated, once {@link #getLevel()} gets called.
     */
    @Getter private final LevelMapping levelMapping = new LevelMapping();

    /**
     * Constructs a new level, of the specified size.
     * @param width The width of the levelMatrix.
     * @param height The height of the levelMatrix.
     */
    Level(int width, int height) {
        this.width = width;
        this.height = height;

        // Initialize the level matrix
        levelMatrix = new ArrayList<>(height);
        for (int y = 0; y < height; y++) {
            List<ArrayList<String>> row = new ArrayList<>(width);
            levelMatrix.add(row);
            for (int x = 0; x < width; x++) {
                row.add(new ArrayList<>(1));
            }
        }
    }

    /**
     * Adds the sprite at the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void addSprite(int x, int y, String sprite) {
        ArrayList<String> sprites = levelMatrix.get(y).get(x);
        sprites.add(sprite);
    }

    /**
     * Sets the sprite at the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void setSprite(int x, int y, String sprite) {
        // TODO add behaviour for cut of areas of the level (for example: fill with solid sprites)
        // TODO should probably return which sprites were previously at this coordinate
        levelMatrix.get(y).set(x, new ArrayList<>(List.of(sprite)));
    }

    /**
     * Sets the specified sprite at any location that has 0 sprites set, in the level matrix.
     * @param sprite The sprite to set (A floor sprite)
     */
    void setSpriteInEmptySpaces(String sprite) {
        for (int y = 0; y < levelMatrix.size(); y++) {
            List<ArrayList<String>> row = levelMatrix.get(y);
            for (int x = 0; x < row.size(); x++) {
                if (row.get(x).isEmpty()) setSprite(x, y, sprite);
            }
        }
    }

    /**
     * Transforms the level matrix into a single string.
     * @return The level matrix as a single String.
     */
    String getLevel() {
        StringBuilder result = new StringBuilder();
        for (List<ArrayList<String>> row : levelMatrix) {
            for (ArrayList<String> sprites : row) {
                result.append(levelMapping.get(sprites));
            }

            result.append('\n');
        }

        return result.substring(0, result.length() - 1);
    }
}
