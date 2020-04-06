package tracks.singlePlayer.florabranchi.persistence;

public enum AvailableGames {
  ALIENS(0, "aliens"),
  DRAGONS(1, "dragons"),
  KATANA(2, "katana"),
  KNIGHT_MAZE(3, "knight_maze"),
  WEIRD_MAZE(4, "weird_maze"),
  DWARF_MAZE(5, "dward_maze"),
  ZELDA(12, "zelda"),
  BUTTERFLIES(13, "butterflies"),
  RACE(15, "race"),
  SPACESHIP(21, "spaceship");


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
