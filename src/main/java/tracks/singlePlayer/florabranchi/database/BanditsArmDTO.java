package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cmab_data")
public class BanditsArmDTO implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int id;

  public int iterations;

  @Column(name = "serialized_object", columnDefinition = "LONGBLOB")
  public BanditArmsData object;

  public String game;

  public BanditsArmDTO(final BanditArmsData object) {
    this.object = object;
  }

  public BanditsArmDTO() {

  }
}
