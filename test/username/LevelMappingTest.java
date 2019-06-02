package username;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class LevelMappingTest {

	private LevelMapping levelMapping;

	@BeforeEach void beforeEach() {
		levelMapping = new LevelMapping();
	}

	@Test void empty() {
		assertThat(levelMapping.getCharMapping()).isEmpty();
	}

	@Test void putOne() {
		String[] spritesInput = {"s1", "s2", "s3"};
		String[] spritesExpected = {"s1", "s2", "s3"};

		putAndAssert('A', spritesInput);

		assertThat(levelMapping.getCharMapping()).containsOnly(entry('A', toArrayList(spritesExpected)));
	}

	@Test void putSameTwice() {
		String[] spritesInput1 = {"s1", "s2", "s3"};
		String[] spritesInput2 = {"s1", "s2", "s3"};
		String[] spritesExpected = {"s1", "s2", "s3"};

		putAndAssert('A', spritesInput1);
		putAndAssert('A', spritesInput2);

		assertThat(levelMapping.getCharMapping()).as("If we try to put the same list of sprites in the map twice,"
				+ "it should only be added once.").containsOnly(entry('A', toArrayList(spritesExpected)));
	}

	@Test void putThree() {
		String[] spritesInput1 = {"s1", "s2", "s3"};
		String[] spritesInput2 = {"s1", "s2", "s3", "s4"};
		String[] spritesInput3 = {"s1", "s2", "s3", "s5"};

		putAndAssert('A', spritesInput1);
		putAndAssert('B', spritesInput2);
		putAndAssert('C', spritesInput3);

		assertThat(levelMapping.getCharMapping()).hasSize(3);
	}

	@Test void gitWithList() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesInput2 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected2 = new ArrayList<>(List.of("s1", "s2", "s3", "s4"));

		putAndAssert(spritesInput1, 'A');
		assertThat(levelMapping.getWith(spritesInput2, "s4")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).as("If we expand the mapping of a list of sprites,"
						+ "a new list of sprites (with the added sprite) should be added.").containsOnly(
				entry('A', spritesExpected1), entry('B', spritesExpected2));
	}

	@Test void getWithChar() {
		String[] spritesInput1 = {"s1", "s2", "s3"};
		String[] spritesExpected1 = {"s1", "s2", "s3"};
		String[] spritesExpected2 = {"s1", "s2", "s3", "s4"};

		putAndAssert('A', spritesInput1);
		assertThat(levelMapping.getWith('A', "s4")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).containsOnly(
				entry('A', toArrayList(spritesExpected1)), entry('B', toArrayList(spritesExpected2)));
	}

	@Test void getWithCharNotFound() {
		String[] spritesInput1 = {"s1", "s2", "s3"};
		String[] spritesExpected1 = {"s1", "s2", "s3"};
		String[] spritesExpected2 = {"s4"};

		putAndAssert('A', spritesInput1);
		assertThat(levelMapping.getWith('Z', "s4")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).as("If the given char does not map to a list,"
				+ "a new list should be created.").containsOnly(entry('A', toArrayList(spritesExpected1)),
				entry('B', toArrayList(spritesExpected2)));
	}

	@Test void gitWithoutList() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesInput2 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected2 = new ArrayList<>(List.of("s1", "s2"));

		putAndAssert(spritesInput1, 'A');
		assertThat(levelMapping.getWithout(spritesInput2, "s3")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).as("If we expand the mapping of a list of sprites,"
				+ "a new list of sprites (with the added sprite) should be added.").containsOnly(
				entry('A', spritesExpected1), entry('B', spritesExpected2));
	}

	@Test void getWithoutChar() {
		String[] spritesInput1 = {"s1", "s2", "s3"};
		String[] spritesExpected1 = {"s1", "s2", "s3"};
		String[] spritesExpected2 = {"s1", "s2"};

		putAndAssert('A', spritesInput1);
		assertThat(levelMapping.getWithout('A', "s3")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).containsOnly(
				entry('A', toArrayList(spritesExpected1)), entry('B', toArrayList(spritesExpected2)));
	}

	@Test void getWithoutCharNotFound() {
		String[] spritesInput1 = {"s1", "s2", "s3"};
		String[] spritesExpected1 = {"s1", "s2", "s3"};
		String[] spritesExpected2 = {};

		putAndAssert('A', spritesInput1);
		assertThat(levelMapping.getWithout('Z', "s3")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).as("If the given char does not map to a list,"
				+ "a new list should be created.").containsOnly(entry('A', toArrayList(spritesExpected1)),
				entry('B', toArrayList(spritesExpected2)));
	}

	@Test void modifyInputList() {
		ArrayList<String> spritesInput = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected = new ArrayList<>(List.of("s1", "s2", "s3"));

		putAndAssert(spritesInput, 'A');
		spritesInput.add("s4");

		assertThat(levelMapping.get('A')).isEqualTo(spritesExpected);
		assertThat(levelMapping.get(spritesExpected)).isEqualTo('A');
	}

	private void putAndAssert(List<String> sprites, char expectedChar) {
		ArrayList<String> spritesExpected = new ArrayList<>(sprites);

		assertThat(levelMapping.get(sprites)).isEqualTo(expectedChar);
		assertThat(levelMapping.get(expectedChar)).isEqualTo(spritesExpected);
		assertThat(levelMapping.get(spritesExpected)).isEqualTo(expectedChar);
		assertThat(sprites).isEqualTo(spritesExpected);
	}

	private void putAndAssert(char expectedChar, String... sprites) {
		String[] spritesExpected = sprites.clone();

		assertThat(levelMapping.get(sprites)).isEqualTo(expectedChar);
		assertThat(levelMapping.get(expectedChar)).isEqualTo(toArrayList(spritesExpected));
		assertThat(levelMapping.get(spritesExpected)).isEqualTo(expectedChar);
		assertThat(sprites).isEqualTo(spritesExpected);
	}

	@SafeVarargs
	@SuppressWarnings("PMD.LooseCoupling")
	private <T> ArrayList<T> toArrayList(T... sprites) {
		return new ArrayList<>(Arrays.asList(sprites));
	}
}
