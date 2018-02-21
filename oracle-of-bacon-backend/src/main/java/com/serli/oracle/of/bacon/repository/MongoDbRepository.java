package com.serli.oracle.of.bacon.repository;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Optional;

public class MongoDbRepository {
    private final MongoCollection<Document> actorCollection;

    public MongoDbRepository() {
        this.actorCollection= new MongoClient("localhost", 27017).getDatabase("workshop").getCollection("actors");
    }

    public Optional<Document> getActorByName(String name) {
        return Optional.ofNullable(this.actorCollection.find(Filters.eq("name", name)).first());
    }
}
