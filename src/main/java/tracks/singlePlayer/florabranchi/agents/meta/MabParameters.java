package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

// slots
public class MabParameters {

  public Map<EMetaParameters, Boolean> values = new HashMap<>();

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof MabParameters)) return false;

    final MabParameters that = (MabParameters) o;

    return Objects.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    return values != null ? values.hashCode() : 0;
  }

  public void addParameter(final EMetaParameters parameter,
                           final boolean value) {
    values.put(parameter, value);
  }

  public boolean getParameter(final EMetaParameters parameter) {
    return values.getOrDefault(parameter, false);
  }

  public void turnOn(final EMetaParameters parameter) {
    values.put(parameter, true);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MabParameters.class.getSimpleName() + "[", "]")
        .add("values=" + values)
        .toString();
  }

  public void randomMutation(final Random random) {
    final List<EMetaParameters> eMetaParameters = new ArrayList<>(values.keySet());
    final EMetaParameters param = eMetaParameters.get(random.nextInt(eMetaParameters.size()));
    boolean value = values.get(param);
    values.put(param, !value);
  }
}
