package tracks.singlePlayer.florabranchi.training;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

public class StateEvaluatorHelper {

  public static int getHeight(final StateObservation stateObservation) {
    return (int) stateObservation.getWorldDimension().getHeight();
  }

  public static int getWidth(final StateObservation stateObservation) {
    return (int) stateObservation.getWorldDimension().getWidth();
  }

  public static StateEvaluatorHelper.ObservableData getNpcData(final StateObservation stateObservation) {
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    final ArrayList<Observation>[] npcPositions = stateObservation.getNPCPositions();
    return getObservableData(npcPositions, avatarPosition);
  }

  public static StateEvaluatorHelper.ObservableData getPortalsData(final StateObservation stateObservation) {
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    final ArrayList<Observation>[] npcPositions = stateObservation.getPortalsPositions();
    return getObservableData(npcPositions, avatarPosition);
  }

  public static StateEvaluatorHelper.ObservableData getResourcesData(final StateObservation stateObservation) {
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    final ArrayList<Observation>[] npcPositions = stateObservation.getResourcesPositions();
    return getObservableData(npcPositions, avatarPosition);
  }

  public static StateEvaluatorHelper.ObservableData getImmovableData(final StateObservation stateObservation) {
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    final ArrayList<Observation>[] immovablePositions = stateObservation.getImmovablePositions();
    return getObservableData(immovablePositions, avatarPosition);
  }

  public static StateEvaluatorHelper.ObservableData getMovablesData(final StateObservation stateObservation) {
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    final ArrayList<Observation>[] npcPositions = stateObservation.getMovablePositions();
    return getObservableData(npcPositions, avatarPosition);
  }

  public static Double getAverageDistance(final ObservableData observableData) {
    return observableData.totalDist / observableData.totalEnemies;
  }

  public static ObservableData getObservableData(final ArrayList<Observation>[] objectsList,
                                                 final Vector2d avatarPosition) {
    double totalEnemies = 0;
    double maxDistance = 0;
    double minDistance = Integer.MAX_VALUE;
    double totalDist = 0;

    Observation furtherObject = null;
    Observation closestObject = null;

    if (objectsList != null && objectsList.length != 0 && objectsList[0] != null) {

      // flat list
      List<Observation> allObservations =
          Arrays.stream(objectsList).sequential()
              .flatMap(List::stream)
              .collect(Collectors.toList());

      if (allObservations.size() != 0) {

        furtherObject = Collections.max(new ArrayList<>(allObservations), Comparator.comparing(c -> c.position.dist(avatarPosition)));
        closestObject = Collections.min(new ArrayList<>(allObservations), Comparator.comparing(c -> c.position.dist(avatarPosition)));

        for (final ArrayList<Observation> objects : objectsList) {
          for (Observation object : objects) {
            double distance = object.position.dist(avatarPosition);
            if (distance > maxDistance) {
              maxDistance = distance;
              furtherObject = object;
            }

            if (distance < minDistance) {
              minDistance = distance;
              closestObject = object;
            }

            totalDist += distance;
            totalEnemies++;
          }
        }
      }
    }

    return new ObservableData(maxDistance, minDistance, totalEnemies, totalDist, furtherObject, closestObject);
  }


  public static class ObservableData {

    public double maxDistance = 0;
    public double minDistance = 0;
    public double totalDist = 0;
    public double totalEnemies;
    public Observation furtherObject;
    public Observation closerObject;

    public ObservableData(final double maxDistance,
                          final double minDistance,
                          final double totalEnemies,
                          final double totalDist,
                          final Observation furtherObject,
                          final Observation closerObject) {
      this.maxDistance = maxDistance;
      this.minDistance = minDistance;
      this.totalDist = totalDist;
      this.furtherObject = furtherObject;
      this.closerObject = closerObject;
      this.totalEnemies = totalEnemies;
    }
  }


}
