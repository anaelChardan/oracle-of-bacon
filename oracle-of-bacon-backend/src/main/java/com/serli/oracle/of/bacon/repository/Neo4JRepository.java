package com.serli.oracle.of.bacon.repository;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Neo4JRepository {
    private final Driver driver;

    public Neo4JRepository() {
        this.driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "root"));
    }

    public List<GraphItem> getConnectionsToKevinBacon(String actorName) {
        Session session = driver.session();

        Transaction t = session.beginTransaction();
        String baconName = "Bacon, Kevin (I)";

        StatementResult result = t.run(
                "MATCH " +
                        "(bc:Actors {name: {baconName}}), (ran:Actors {name: {relatedActorName}})," +
                        "p = shortestPath((bc)-[:PLAYED_IN*]-(ran)) " +
                        "WITH p WHERE length(p) > 1 " +
                        "RETURN p",
                parameters("baconName", baconName, "relatedActorName", actorName)
                );

        List<Path> paths =  result
                .list()
                .stream()
                .flatMap(records -> records.values().stream().map(Value::asPath))
                .collect(Collectors.toList());

        List<GraphNode> nodes = paths
                .stream()
                .map(path -> iteratorToList(path.nodes().iterator()))
                .flatMap(ns -> ns.stream().map(this::toGraphNode))
                .collect(Collectors.toList());

        List<GraphEdge> edges = paths
                .stream()
                .map(path -> iteratorToList(path.relationships().iterator()))
                .flatMap(es -> es.stream().map(this::toGraphEdge))
                .collect(Collectors.toList());

        List<GraphItem> items = new ArrayList<>(nodes);
        items.addAll(edges);

        return items;
    }

    private GraphNode toGraphNode(Node n) {
        String type = n.labels().iterator().next();
        String property = type.equals("Actors") ? "name" : "title";

        return new GraphNode(n.id(), n.get(property).asString(), type);
    }

    private GraphEdge toGraphEdge(Relationship relationship) {
        return new GraphEdge(
               relationship.id(), relationship.startNodeId(), relationship.endNodeId(), relationship.type()
        );
    }

    private  <T> List<T> iteratorToList(Iterator<T> iterator) {
        List<T> list = new ArrayList();
        iterator.forEachRemaining(list::add);

        return list;
    }

    public static abstract class GraphItem {
        public final long id;

        private GraphItem(long id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GraphItem graphItem = (GraphItem) o;

            return id == graphItem.id;
        }

        @Override
        public int hashCode() {
            return (int) (id ^ (id >>> 32));
        }
    }

    private static class GraphNode extends GraphItem {
        public final String type;
        public final String value;

        public GraphNode(long id, String value, String type) {
            super(id);
            this.value = value;
            this.type = type;
        }
    }

    private static class GraphEdge extends GraphItem {
        public final long source;
        public final long target;
        public final String value;

        public GraphEdge(long id, long source, long target, String value) {
            super(id);
            this.source = source;
            this.target = target;
            this.value = value;
        }
    }
}
