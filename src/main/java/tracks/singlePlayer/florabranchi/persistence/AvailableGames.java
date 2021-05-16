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
  G_41(41, "fireman"),
  G_42(42, "firestorms"),
  G_82(82, "run"),
  G_44(44, "frogs"),
  G_45(45, "garbagecollector"),
  G_46(46, "ghostbuster"),
  G_47(47, "glow"),
  G_48(48, "grow"),
  G_49(49, "gymkhana"),
  G_50(50, "hungrybirds"),
  G_51(51, "iceandfire"),
  G_52(52, "ikaruga"),
  G_53(53, "infection"),
  G_54(54, "intersection"),
  G_55(55, "islands"),
  G_56(56, "jaws"),
  G_57(57, "killBillVol1"),
  G_58(58, "labyrinth"),
  G_59(59, "labyrinthdual"),
  G_60(60, "lasers"),
  G_70(70, "painter"),
  G_81(81, "roguelike"),
  G_77(77, "seaquest"),
  G_91(91, "surviving_zombies"),
  G_108(108, "zelda"),
  G_68(68, "pacman"),
  G_67(61, "overload"),
  G_71(71, "plants"),
  G_73(73, "pokemon");

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
