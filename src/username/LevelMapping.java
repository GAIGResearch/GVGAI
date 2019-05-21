package username;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A class containing a level mapping, which maps characters to lists of sprites.
 */
@SuppressWarnings("PMD.LooseCoupling")
class LevelMapping {

    /**
     * Maps characters to lists of sprites. Reverse of {@link #spritesMapping}.
     */
    @Getter private final HashMap<Character, ArrayList<String>> charMapping = new HashMap<>();

    /**
     * Maps lists of sprites to characters. Reverse of {@link #charMapping}.
     */
    private final HashMap<List<String>, Character> spritesMapping = new HashMap<>();

    /**
     * The char that gets used, when a new mapping needs to be created.
     */
    private char mappingChar = 'A';

    /**
     * Gets from the charMapping.
     * @param character A character.
     * @return The list of sprites to which the specified character is mapped.
     */
    ArrayList<String> get(char character) {
        return charMapping.get(character);
    }

    /**
     * Maps the given list of sprites to a char, if it wasn't in a mapping already.
     * @param sprites List of Strings representing sprites.
     * @return The char the list of sprites got mapped to.
     */
    char get(final ArrayList<String> sprites) {
        // If this mapping already exists, return the char
        if (spritesMapping.containsKey(sprites)) return spritesMapping.get(sprites);

        // Copy the list of sprites
        ArrayList<String> newSprites = new ArrayList<>(sprites);

        // Put the new mapping in both maps
        charMapping.put(mappingChar, newSprites);
        spritesMapping.put(newSprites, mappingChar);

        // Return and update the mapping character
        char character = mappingChar;
        mappingChar++;
        return character;
    }

    /**
     * Given a list of sprites, adds a sprite to that list and creates a mapping for that new list,
     * if there didn't exist one already.
     * @param sprites A list of Strings representing sprites.
     * @param sprite A String representing a sprite.
     * @return The char the list of sprites got mapped to.
     */
    char expandMapping(ArrayList<String> sprites, String sprite) {
        // Get the new set of sprites
        ArrayList<String> newSprites = new ArrayList<>(sprites);
        newSprites.add(sprite);

        // Create a mapping for the new set of sprites
        return get(newSprites);
    }

    /**
     * Given a character which is mapped to a list of sprites, adds a sprite to that list and creates a mapping for that
     * new list, if there didn't exists such a mapping already. If the character is not mapped to a list of sprites,
     * this method will create a new list with only the given sprite
     * @param character A character which is mapped to a list of sprites.
     * @param sprite A String representing a sprite.
     * @return The char the list of sprites got mapped to.
     */
    char expandMapping(char character, String sprite) {
        return expandMapping(charMapping.getOrDefault(character, new ArrayList<>()), sprite);
    }
}
