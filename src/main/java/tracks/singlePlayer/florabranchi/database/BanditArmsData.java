package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Embeddable;

import tracks.singlePlayer.florabranchi.agents.meta.EMetaParameters;
import tracks.singlePlayer.florabranchi.agents.meta.GlobalMabData;
import tracks.singlePlayer.florabranchi.agents.meta.LocalMabData;
import tracks.singlePlayer.florabranchi.agents.meta.MabParameters;

public class BanditArmsData implements Serializable {

  public List<ArmData> armDataList = new ArrayList<>();

  public List<LocalArmData> localArmData = new ArrayList<>();

  public Map<EMetaParameters, LocalMabData> getLocalMabs() {

    Map<EMetaParameters, LocalMabData> map = new HashMap<>();
    localArmData.forEach(entry -> {
      map.put(entry.metaParameters, new LocalMabData(entry.localMabData));
    });
    return map;
  }

  public Map<MabParameters, GlobalMabData> getGlobalMabs() {
    Map<MabParameters, GlobalMabData> map = new HashMap<>();
    armDataList.forEach(entry -> map.put(entry.toMabParameter(), entry.toGlobalMab()));
    return map;
  }

  public void updateArms(Map<EMetaParameters, LocalMabData> localMabs,
                         Map<MabParameters, GlobalMabData> globalMab) {
    this.localArmData.clear();
    this.armDataList.clear();
    localMabs.forEach((key, value) -> localArmData.add(new LocalArmData(key, value)));
    globalMab.forEach((key, value) -> armDataList.add(new ArmData(key, value)));
  }

  public BanditArmsData(final List<ArmData> armDataList, final List<LocalArmData> localArmData) {
    this.armDataList = armDataList;
    this.localArmData = localArmData;
  }

  public BanditArmsData() {
  }

  @Embeddable
  static
  class ArmData implements Serializable {

    public boolean rawGameScore;
    public boolean macroActions;
    public boolean lossAvoidance;
    public boolean earlyInitialization;
    public boolean selectHighestScoreChild;
    public double totalRewards;
    public int timesSelected;

    public ArmData(final MabParameters mabParameters,
                   final GlobalMabData globalMabData) {
      rawGameScore = mabParameters.getParameter(EMetaParameters.RAW_GAME_SCORE);
      macroActions = mabParameters.getParameter(EMetaParameters.MACRO_ACTIONS);
      lossAvoidance = mabParameters.getParameter(EMetaParameters.LOSS_AVOIDANCE);
      earlyInitialization = mabParameters.getParameter(EMetaParameters.EARLY_INITIALIZATION);
      selectHighestScoreChild = mabParameters.getParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD);
      totalRewards = globalMabData.totalRewards;
      timesSelected = globalMabData.timesSelected;
    }

    public MabParameters toMabParameter() {
      MabParameters mabParameters = new MabParameters();
      mabParameters.addParameter(EMetaParameters.RAW_GAME_SCORE, rawGameScore);
      mabParameters.addParameter(EMetaParameters.MACRO_ACTIONS, macroActions);
      mabParameters.addParameter(EMetaParameters.LOSS_AVOIDANCE, lossAvoidance);
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, earlyInitialization);
      mabParameters.addParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD, selectHighestScoreChild);
      return mabParameters;
    }

    public GlobalMabData toGlobalMab() {
      GlobalMabData data = new GlobalMabData();
      data.timesSelected = this.timesSelected;
      data.totalRewards = this.totalRewards;
      return data;
    }

    public ArmData() {

    }
  }

  static
  class LocalArmData implements Serializable {

    Map<Boolean, LocalMabData.LocalMabInfo> localMabData = new HashMap<>();

    public EMetaParameters metaParameters;
    public String metaParameterString;

    public LocalArmData(final EMetaParameters metaParameters,
                        final LocalMabData localMabData) {
      this.metaParameters = metaParameters;
      this.metaParameterString = metaParameters.toString();

      localMabData.localMabData.forEach(
          (key, value) -> {
            this.localMabData.put(key, value);
          }
      );
    }

    public LocalArmData() {

    }
  }


}
