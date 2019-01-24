package agents.TeamTopbug.TreeSearch;

import java.util.ArrayList;
import java.util.Comparator;

import core.game.StateObservation;
import ontology.Types;
import agents.TeamTopbug.Agent;
import agents.TeamTopbug.GameInfo;
import agents.TeamTopbug.Node;
import agents.TeamTopbug.NodePool;
import agents.TeamTopbug.SortedList;

public class GA extends TreeSearch {
	public static int GA_POPULATION_SIZE = 100; // 10,25,50,100
	public static int GA_MAX_LENGTH = 4;
	public static double GA_MUTATION_CHANCE = 1.0;

	public SortedList<Individual> population;
	protected Comparator<Individual> comparator = null;

	public GA(Node origin) {
		super(origin);

		this.comparator = new Comparator<Individual>() {
			@Override
			public int compare(Individual o1, Individual o2) {
				double f1 = o1.fitness;
				double f2 = o2.fitness;
				if (f1 < f2) {
					return 1;
				} else if (f1 > f2) {
					return -1;
				} else {
					return 0;
				}
			}
		};
		population = new SortedList<Individual>(comparator);

		// initial population
		for (int i = 0; i < GA_POPULATION_SIZE; i++) {
			Individual random = Individual.Random(GA_MAX_LENGTH);
			random.updateFitness(this.origin);
			population.add(random);
		}

	}

	@Override
	public void search() {
		while (!Agent.anyTime.isTimeOver()) {
			Agent.anyTime.updatePerLoop();
			anyTimeGeneration();
		}
	}

	@Override
	public void roll(Node origin) {
		for (Individual ind : population) {
			ind.remove(0);
			ind.add(Individual.RandomAction());
		}
		this.origin = origin;
	}

	public static class Individual extends ArrayList<Types.ACTIONS> {
		public double fitness;
		protected Node leaf = null;

		public Individual() {
			super(GA_MAX_LENGTH);
		}

		public static Individual Random(int length) {
			Individual ind = new Individual();
			for (int i = 0; i < length; i++) {
				ind.add(RandomAction());
			}
			return ind;
		}

		public static Types.ACTIONS RandomAction() {
			return GameInfo.actions[Agent.random.nextInt(GameInfo.NUM_ACTIONS)];
		}

		protected Node tryToGetToLeaf(Node origin) {
			Node current = origin;
			for (Types.ACTIONS action : this) {
				if (current.children.containsKey(action)) {
					current = current.children.get(action);
				} else {
					// no terminal leaf
					return null;
				}
			}
			return current;
		}

		public void updateFitness(Node origin) {
			Node current = origin;
			double sum = 0;

			StateObservation state = origin.state;

			// try to get to leaf of this sequence
			if (leaf == null) {
				leaf = tryToGetToLeaf(origin);
			}

			if (leaf != null) {
				// terminal leaf found
				// only update if not visited often
				if (leaf.nVisits < 1) {
					// this path is already present in the tree, just move further
					current = origin;
					current.state = state.copy();
					for (Types.ACTIONS action : this) {
						current = current.children.get(action);
						current.update();
						current.updateAverageReward();
						current.updateBestReward();
					}
				} /* else: no update - just take old values */
			} else {
				// some parts of the tree must be expanded first
				current = origin;
				current.state = state.copy();
				for (Types.ACTIONS action : this) {
					if (current.children.containsKey(action)) {
						// this path is already present in the tree, just move further
						current = current.children.get(action);
						current.update();
						current.updateAverageReward();
						current.updateBestReward();
					} else {
						// expand the tree
						Node child = NodePool.get().init(action, current); // also updates the Node
						current.children.put(action, child);
						current = child;
					}
				}
				leaf = current;
			}

			current = origin;
			for (Types.ACTIONS action : this) {
				// this path is already present in the tree, just move further
				current = current.children.get(action);
				sum += current.averageReward;
			}
			fitness = sum / size();

			origin.state = state;
		}

		public static Individual Recombine(Individual a, Individual b) {
			// one point crossover
			assert a.size() == b.size();
			int r = Agent.random.nextInt(a.size());
			Individual child = new Individual();
			child.addAll(a.subList(0, r));
			child.addAll(b.subList(r, b.size()));
			return child;
		}

		public static Individual Mutate(Individual a) {
			// one point mutate
			int r = Agent.random.nextInt(a.size());
			Individual child = new Individual();
			child.addAll(a);
			child.set(r, RandomAction());
			return child;
		}
	}

	int selectIndividual(int n) {
		int r = (int) ((Agent.random.nextGaussian() / 2) * (n / 2));
		if (r < 0)
			return 0;
		if (r > n - 1)
			return n - 1;
		return r;
	}

	public void anyTimeGeneration() {
		population.remove(population.size() - 1);
		int n = population.size();
		Individual child = null;
		if (Agent.random.nextDouble() < GA_MUTATION_CHANCE) {
			// mutate
			int r = selectIndividual(n);

			child = Individual.Mutate(population.get(r));
		} else {
			// recombination
			int r1 = selectIndividual(n);
			int r2 = selectIndividual(n);
			child = Individual.Recombine(population.get(r1), population.get(r2));
		}
		child.updateFitness(this.origin);

		population.add(child);
	}

}
