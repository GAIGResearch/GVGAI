package tracks.singlePlayer.florabranchi.agents;

import java.io.IOException;
import java.util.logging.Logger;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

public abstract class AbstractAgent extends AbstractPlayer {

  protected final static Logger LOGGER = Logger.getLogger("GVGAI_BOT");

  public PropertyLoader propertyLoader;

  protected AbstractAgent(final StateObservation stateObs,
                          final ElapsedCpuTimer elapsedTimer) {
    super();

    try {
      propertyLoader = new PropertyLoader(getPropertyPath());

    } catch (IOException ex) {
      LOGGER.severe("Error loading properties");
    }
  }


  protected abstract String getPropertyPath();

}
