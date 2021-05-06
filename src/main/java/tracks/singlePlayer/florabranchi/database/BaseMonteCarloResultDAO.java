package tracks.singlePlayer.florabranchi.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class BaseMonteCarloResultDAO {

  static EntityManagerFactory entityManagerFactory = null;

  public BaseMonteCarloResultDAO() {

    if (entityManagerFactory == null) {
      entityManagerFactory = Persistence.createEntityManagerFactory("base_mcts_results");
    }
  }

  public void save(BaseMonteCarloResult baseMonteCarloResult) {
    EntityManager manager = entityManagerFactory.createEntityManager();
    manager.getTransaction().begin();
    manager.persist(baseMonteCarloResult);
    manager.getTransaction().commit();
    manager.close();
  }
}
