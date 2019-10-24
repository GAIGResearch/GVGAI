package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;
import java.util.ArrayList;

public class WeightVector extends ArrayList<Double> implements Serializable {

  public WeightVector() {
    super();
  }

  public double[] castToArray() {

    double[] result = new double[this.size()];
    int i = 0;
    for (Double weight : this) {
      result[i] = weight;
    }
    return result;
  }

}
