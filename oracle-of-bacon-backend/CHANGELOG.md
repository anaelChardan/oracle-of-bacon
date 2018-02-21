# Rendu Anaël CHARDAN et Romain DAVAZE

- [X] import-tool
    `cd ~/progs/neo4j-community-3.3.2 && rm -rf data/* && bin/neo4j-admin import --nodes:Movies ~/progs/data-oracle-of-bacon/movies.csv --nodes:Actors ~/progs/data-oracle-of-bacon/actors.csv --relationships ~progs/data-oracle-of-bacon/roles.csv`
- [X] Implémenter l'Oracle de Bacon à l'aide de Neo4J dans la méthode com.serli.oracle.of.bacon.repository.Neo4JRepository#getConnectionsToKevinBacon
- [X] Implémenter la gestion du last 10 search à l'aide de Redis dans la méthode com.serli.oracle.of.bacon.repository.RedisRepository#getLastTenSearches
- [X] Importer les données à l'aide de ElasticSearch dans com.serli.oracle.of.bacon.loader.elasticsearch.CompletionLoader (les liens suivants pourront vous aider : search, mapping et suggest)
- [X] Implémenter la suggestion sur le nom des acteurs dans com.serli.oracle.of.bacon.repository.ElasticSearchRepository#getActorsSuggests
- [X] Implémenter la recherche des acteurs par nom à l'aide de MongoDB dans com.serli.oracle.of.bacon.repository.MongoDbRepository#getActorByName

Vous pouvez faire `make list` pour voir comment lancer le project back
Le Mot de passe de neo4j doit être root ou alors, modifier la conf dans le repository correspondant.