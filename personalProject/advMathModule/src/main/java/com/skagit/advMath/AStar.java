package com.skagit.advMath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * We use a PriorityQueue in this version of AStar, and allow multiple entries
 * in the open set for the same city.
 */
public class AStar {
    final public City _startCity;
    final public City _stopCity;
    final public Node _winningNode;

    public interface City {
	double getStraightShotEst(City city);

	double getEdgeWeight(City city);

	Collection<City> computeNeighbors();

	String getName();
    }

    public class Node {
	/** User needs to know the public fields to piece together the path. */
	final public City _city;
	final private double _straightShotToStop;
	final private Node _predecessor;
	final public double _dFromStart;
	final private double _estViaMeToStop;

	private Node(final City city, final Node predecessor, final double dFromStart) {
	    _city = city;
	    _straightShotToStop = _city.getStraightShotEst(_stopCity);
	    _predecessor = predecessor;
	    _dFromStart = dFromStart;
	    _estViaMeToStop = _dFromStart + _straightShotToStop;
	}

	private int getNStepsFromStart() {
	    return _predecessor == null ? 0 : (1 + _predecessor.getNStepsFromStart());
	}

	public Node[] getNodeSequence() {
	    final int nNodes = 1 + getNStepsFromStart();
	    final Node[] nodes = new Node[nNodes];
	    int k = 0;
	    for (Node n = this; n != null; n = n._predecessor) {
		nodes[nNodes - 1 - k] = n;
		++k;
	    }
	    return nodes;
	}

	public String getBigString() {
	    String s = "";
	    boolean gotOne = false;
	    double cumWt = 0;
	    for (final Node n : getNodeSequence()) {
		final Node p = n._predecessor;
		if (p != null) {
		    final String pName = p._city.getName();
		    final double ew = p._city.getEdgeWeight(n._city);
		    cumWt += ew;
		    if (gotOne) {
			s += "\n";
		    }
		    gotOne = true;
		    s += String.format("%s→%s (EdgWt[%f],CumWt[%f]) DFromStart[%s] " + "StrtShotToStp[%f] EstViaMe[%f]", //
			    pName, n._city.getName(), ew, cumWt, n._dFromStart, n._straightShotToStop,
			    n._estViaMeToStop);
		}
	    }
	    return s;
	}
    }

    public AStar(final City startCity, final City stopCity) {
	_startCity = startCity;
	_stopCity = stopCity;
	/** Compute _winningNode. */
	final HashSet<City> closed = new HashSet<>();
	final int initialCapacity = 128;
	final PriorityQueue<Node> sortedOpenNodes = new PriorityQueue<>(initialCapacity, new Comparator<Node>() {
	    @Override
	    public int compare(final Node o1, final Node o2) {
		if (o1._estViaMeToStop != o2._estViaMeToStop) {
		    return o1._estViaMeToStop < o2._estViaMeToStop ? -1 : 1;
		}
		return o1._city == _stopCity ? -1 : (o2._city == _stopCity ? 1 : 0);
	    }
	});
	final Node startNode = new Node(_startCity, null, 0.0);
	sortedOpenNodes.add(startNode);
	Node winningNode = null;
	for (Node node = sortedOpenNodes.poll();; node = sortedOpenNodes.poll()) {
	    if (node._city == _stopCity) {
		winningNode = node;
		break;
	    }
	    /** If it's already closed, there's nothing to do. */
	    final City city = node._city;
	    if (!closed.add(city)) {
		continue;
	    }
	    /**
	     * For each neighbor, ignore it if it is done. Otherwise, make an entry for it
	     * in the priority queue.
	     */
	    for (final City neighbor : city.computeNeighbors()) {
		if (closed.contains(neighbor)) {
		    continue;
		}
		final double dFromStartToNeighborViaNode = node._dFromStart + city.getEdgeWeight(neighbor);
		final Node newNode = new Node(neighbor, node, dFromStartToNeighborViaNode);
		sortedOpenNodes.add(newNode);
	    }
	}
	_winningNode = winningNode;
    }

    private static class SimpleCity implements City {
	final String _name;
	final TreeMap<SimpleCity, Double> _edgeWeights;
	final double _straightShotToEnd;

	public SimpleCity(final String name, final double straightShotToEnd) {
	    _name = name;
	    _edgeWeights = new TreeMap<>(new Comparator<SimpleCity>() {
		@Override
		public int compare(final SimpleCity o1, final SimpleCity o2) {
		    return o1._name.compareTo(o2._name);
		}
	    });
	    _straightShotToEnd = straightShotToEnd;
	}

	private void addCity(final SimpleCity city, final double edgeWeight) {
	    _edgeWeights.put(city, edgeWeight);
	}

	@Override
	public double getStraightShotEst(final City goal) {
	    return _straightShotToEnd;
	}

	@Override
	public double getEdgeWeight(final City city) {
	    final Double d = _edgeWeights.get(city);
	    return d == null ? Double.MAX_VALUE : d;
	}

	@Override
	public Collection<City> computeNeighbors() {
	    return new ArrayList<>(_edgeWeights.keySet());
	}

	@Override
	public String getName() {
	    return _name;
	}

	@Override
	public int hashCode() {
	    return _name.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
	    if (!(o instanceof City)) {
		return false;
	    }
	    final City city = (City) o;
	    return getName().equals(city.getName());
	}
    }

    public static void main(final String[] args) {
	/**
	 * Build the network of Cities (in this case, CityA's), and identify startCity
	 * and stopCity.
	 */
	final boolean useEdges1 = false;
	final String[][] edges;
	if (useEdges1) {
	    edges = new String[][] { //
		    /**
		     * The 4th entry indicates that this is a new SimpleCity. It is the
		     * straight-line distance to stopCity. Note that stopCity is itself a SimpleCity
		     * and is the only one that has a straight-line distance to stopCity of 0.0. We
		     * don't need a straight-line distance from startCity.
		     */
		    { "?", "a", "1.5", "Nan" }, //
		    { "?", "d", "2.0" }, //
		    { "a", "b", "2.0", "4.0" }, //
		    { "b", "c", "3.0", "2.0" }, //
		    { "c", "!", "4.0", "4.0" }, //
		    { "d", "e", "3.0", "4.5" }, //
		    { "e", "!", "2.0", "2.0" }, //
		    { "!", null, null, "0.0" } //
	    };
	} else {
	    edges = new String[][] { //
		    { "?", "a", "1.0", "Nan" }, //
		    { "?", "b", "5.0" }, //
		    { "a", "!", "10.0", "1.0" }, //
		    { "b", "!", "1.0", "1.0" }, //
		    { "!", null, null, "0.0" } //
	    };
	}
	SimpleCity startCity = null;
	SimpleCity stopCity = null;
	{
	    final TreeMap<String, SimpleCity> simpleCities = new TreeMap<>();
	    /** First, build the list of SimpleCities. */
	    for (final String[] edge : edges) {
		final String cityName = edge[0];
		if (simpleCities.isEmpty()) {
		    startCity = new SimpleCity(cityName, Double.NaN);
		    simpleCities.put(cityName, startCity);
		} else if (edge.length > 3) {
		    final double straightShotToEnd = Double.parseDouble(edge[3]);
		    final SimpleCity simpleCity = new SimpleCity(cityName, straightShotToEnd);
		    if (simpleCities.put(cityName, simpleCity) != null) {
			System.exit(18);
		    }
		    stopCity = simpleCity;
		}
	    }
	    /** Now add the edges. */
	    for (final String[] edge : edges) {
		if (edge[1] == null || edge[2] == null) {
		    /** Just an entry to introduce a SimpleCity. */
		    continue;
		}
		final SimpleCity simpleCity0 = simpleCities.get(edge[0]);
		final SimpleCity simpleCity1 = simpleCities.get(edge[1]);
		final double edgeWeight = Double.parseDouble(edge[2]);
		simpleCity0.addCity(simpleCity1, edgeWeight);
		simpleCity1.addCity(simpleCity0, edgeWeight);
	    }
	}
	/** Solve the problem. */
	System.out.printf("%s", new AStar(startCity, stopCity)._winningNode.getBigString());
    }
}
