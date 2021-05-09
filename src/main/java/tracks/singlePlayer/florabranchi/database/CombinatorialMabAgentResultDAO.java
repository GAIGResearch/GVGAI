package tracks.singlePlayer.florabranchi.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class CombinatorialMabAgentResultDAO {

  static EntityManagerFactory entityManagerFactory = null;

  public CombinatorialMabAgentResultDAO() {

    if (entityManagerFactory == null) {
      entityManagerFactory = Persistence.createEntityManagerFactory("cmab_mcts_results");
    }
  }

  public void save(CombinatorialMabAgentResult combinatorialMabAgentResult) {
    EntityManager manager = entityManagerFactory.createEntityManager();
    manager.getTransaction().begin();
    manager.persist(combinatorialMabAgentResult);
    manager.getTransaction().commit();
    manager.close();
  }
}
