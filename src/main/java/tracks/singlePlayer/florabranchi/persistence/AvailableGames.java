package tracks.singlePlayer.florabranchi.persistence;

public enum AvailableGames {
  G_1(0, "aliens"),
  G_2(1, "angelsdemons"),
  G_3(2, "assemblyline"),
  G_4(3, "avoidgeorge"),
  G_5(4, "bait"),
  G_6(5, "beltmanager"),
  G_7(6, "blacksmoke"),
  G_72(7, "boloadventures"),
  G_8(8, "bomber"),
  G_9(9, "bomberman"),
  G_10(10, "boulderchase"),
  G_11(11, "boulderdash"),
  G_12(12, "brainman"),
  G_13(13, "butterflies"),
  G_14(14, "cakybaky"),
  G_15(15, "camelRace"),
  G_16(16, "catapults"),
  G_17(17, "chainreaction"),
  G_18(18, "chase"),
  G_19(19, "chipschallenge"),
  G_20(20, "chopper"),
  G_21(21, "circuit"),
  G_22(22, "clusters"),
  G_23(23, "colourescape"),
  G_24(24, "cookmepasta"),
  G_25(25, "cops"),
  G_26(26, "crossfire"),
  G_27(27, "defem"),
  G_28(28, "defender"),
  G_29(29, "deflection"),
  G_30(30, "digdug"),
  G_31(31, "donkeykong"),
  G_32(32, "doorkoban"),
  G_33(33, "dungeon"),
  G_34(34, "eggomania"),
  G_35(35, "eighthpassenger"),
  G_36(36, "enemycitadel"),
  G_37(37, "escape"),
  G_38(38, "explore"),
  G_39(39, "factorymanager"),
  G_40(40, "firecaster"),
  G_44(44, "frogs");

  private Integer id;
  private String name;

  AvailableGames(Integer value, String name) {
    this.id = value;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static AvailableGames fromId(Integer id) {
    for (AvailableGames at : AvailableGames.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }

  public static AvailableGames fromName(String name) {
    for (AvailableGames at : AvailableGames.values()) {
      if (at.getName().equals(name)) {
        return at;
      }
    }
    return null;
  }

}
