package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cmab_data")
public class BanditsArmDTO implements Serializable {

  @Id
  @GeneratedValue
  public int id;

  @Column(name = "serialized_object", columnDefinition = "LONGBLOB")
  public BanditArmsData object;

  public BanditsArmDTO(final BanditArmsData object) {
    this.object = object;
  }
}
