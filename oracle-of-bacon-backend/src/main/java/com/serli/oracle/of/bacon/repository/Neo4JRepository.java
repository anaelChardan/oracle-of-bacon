package com.serli.oracle.of.bacon.repository;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Entity;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

        return result
                .list()
                .stream()
                .flatMap(record -> record.values().stream().map(Value::asPath))
                .flatMap(p -> toGraphItems(p).stream())
                .collect(Collectors.toList());
    }

    private List<GraphItem> toGraphItems(Path path) {
        List<GraphItem> graphItems = toGraphItem(path.nodes(), this::toGraphItem);
        graphItems.addAll(toGraphItem(path.relationships(), this::toGraphItem));

        return graphItems;
    }

    private <T> List<GraphItem> toGraphItem(
            Iterable<T> iterable,
            Function<T, GraphItem> toGraphItem) {

        return StreamSupport
                .stream(iterable.spliterator(), false)
                .map(toGraphItem)
                .collect(Collectors.toList());
    }

    private GraphItem toGraphItem(Node n) {
        String type = n.labels().iterator().next();
        String property = type.equals("Actors") ? "name" : "title";

        return new GraphNode(n.id(), n.get(property).asString(), type);
    }

    private GraphItem toGraphItem(Relationship relationship) {
        return new GraphEdge(
               relationship.id(), relationship.startNodeId(), relationship.endNodeId(), relationship.type()
        );
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
