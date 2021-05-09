package tracks.singlePlayer.florabranchi.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


public class BanditArmsDataDAO {

  static EntityManagerFactory entityManagerFactory = null;

  public BanditArmsDataDAO() {
    if (entityManagerFactory == null) {
      entityManagerFactory = Persistence.createEntityManagerFactory("cmab_bandit_data");
    }
  }

  public void save(BanditArmsData banditArmsData) {
    EntityManager manager = entityManagerFactory.createEntityManager();
    manager.getTransaction().begin();
    manager.persist(banditArmsData);
    manager.getTransaction().commit();
    manager.close();
  }

  public void update(BanditArmsData banditArmsData) {
    EntityManager manager = entityManagerFactory.createEntityManager();
    BanditArmsData banditArmsData1 = (BanditArmsData) manager.find(BanditArmsData.class, banditArmsData.id);
    banditArmsData1.localArmData = banditArmsData.localArmData;
    banditArmsData1.armDataList = banditArmsData.armDataList;
    manager.getTransaction().begin();
    manager.getTransaction().commit();
    manager.close();
  }

  public BanditArmsData get(int id) {
    EntityManager manager = entityManagerFactory.createEntityManager();
    final BanditArmsData banditArmsData1 = (BanditArmsData) manager.find(BanditArmsData.class, id);
    if (banditArmsData1 != null) {
      banditArmsData1.armDataList.size();
      banditArmsData1.localArmData.size();
    }

    manager.close();
    return banditArmsData1;


  }
}
