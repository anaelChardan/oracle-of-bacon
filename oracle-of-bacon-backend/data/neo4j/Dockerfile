FROM neo4j:latest

ENV NEO4J_AUTH "none"

WORKDIR /var/lib/neo4j/

COPY imdb-data import/
COPY docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 7687 7474

ENTRYPOINT ["/docker-entrypoint.sh"]