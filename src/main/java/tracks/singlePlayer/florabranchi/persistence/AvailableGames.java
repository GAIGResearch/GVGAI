package tracks.singlePlayer.florabranchi.persistence;

public enum AvailableGames {
  ALIENS(1, "aliens"),
  DRAGONS(2, "dragons");


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
