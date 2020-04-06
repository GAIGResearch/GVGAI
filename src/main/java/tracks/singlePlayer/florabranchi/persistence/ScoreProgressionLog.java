package tracks.singlePlayer.florabranchi.persistence;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class ScoreProgressionLog extends AbstractLog implements Serializable {

  DecimalFormat df = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance());

  public ScoreProgressionLog(final String filePath) {
    super(filePath);
  }

  public int addEpisode(final double gameScore) {
    int lastEpisode;
    if (trainingLog.isEmpty()) {
      lastEpisode = 0;
    } else {
      final String lastResult = trainingLog.get(trainingLog.size() - 1);
      String[] parsedResult = lastResult.split(",");
      lastEpisode = Integer.parseInt(parsedResult[0]);
    }

    String doubleWithCommas = df.format(gameScore);
    trainingLog.add(String.format("%s,%s", lastEpisode + 1, doubleWithCommas));
    return lastEpisode;
  }
}
