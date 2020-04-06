package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;

public class PossibleHarmfulSprite implements Serializable {

  public int category;

  public int type;

  public PossibleHarmfulSprite(final int category, final int type) {
    this.category = category;
    this.type = type;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PossibleHarmfulSprite that = (PossibleHarmfulSprite) o;

    if (category != that.category) return false;
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = category;
    result = 31 * result + type;
    return result;
  }
}
