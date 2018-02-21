package com.serli.oracle.of.bacon.api;

import com.serli.oracle.of.bacon.repository.ElasticSearchRepository;
import com.serli.oracle.of.bacon.repository.MongoDbRepository;
import com.serli.oracle.of.bacon.repository.Neo4JRepository;
import com.serli.oracle.of.bacon.repository.RedisRepository;
import net.codestory.http.annotations.Get;
import net.codestory.http.convert.TypeConvert;
import org.bson.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class APIEndPoint {
    private final Neo4JRepository neo4JRepository;
    private final ElasticSearchRepository elasticSearchRepository;
    private final RedisRepository redisRepository;
    private final MongoDbRepository mongoDbRepository;

    public APIEndPoint() {
        neo4JRepository = new Neo4JRepository();
        elasticSearchRepository = new ElasticSearchRepository();
        redisRepository = new RedisRepository();
        mongoDbRepository = new MongoDbRepository();
    }

    @Get("bacon-to?actor=:actorName")
    public String getConnectionsToKevinBacon(String actorName) {
        try {
            this.redisRepository.addSearch(java.net.URLDecoder.decode(actorName, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
        }

        List<AbstractMap.SimpleEntry<String, Neo4JRepository.GraphItem>> result = this
                .neo4JRepository
                .getConnectionsToKevinBacon(actorName)
                .stream().map(item -> new AbstractMap.SimpleEntry<>("data", item))
                .collect(Collectors.toList());

        return TypeConvert.toJson(result);
    }

    @Get("suggest?q=:searchQuery")
    public List<String> getActorSuggestion(String searchQuery) throws IOException {
        return this.elasticSearchRepository.getActorsSuggests(searchQuery);
    }

    @Get("last-searches")
    public List<String> last10Searches() {
        return redisRepository.getLastTenSearches();
    }

    @Get("actor?name=:actorName")
    public String getActorByName(String actorName) {
        return this.mongoDbRepository.getActorByName(actorName).map(Document::toJson).orElse("");
    }
}
