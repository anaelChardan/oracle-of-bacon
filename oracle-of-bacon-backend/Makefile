#=== Oracle-of-beacon-back helper ===

# Styles
YELLOW=$(shell echo "\033[00;33m")
RED=$(shell echo "\033[00;31m")
RESTORE=$(shell echo "\033[0m")

CURRENT_DIR := $(shell pwd)

.PHONY: list
list:
	@echo ""
	@echo "Oracle-of-beacon-back available targets:"
	@echo ""
	@echo "  $(YELLOW)boot-databases$(RESTORE)            > Boot the project databases"
	@echo "  $(YELLOW)compile$(RESTORE)                   > Compile the project"
	@echo "  $(YELLOW)import-data-to-es$(RESTORE)         > Import Data to Elasticsearch"


.PHONY: boot-databases
boot-databases:
	docker-compose up -d

.PHONY: compile
compile:
	./gradlew run

.PHONY: import-data-to-es
import-data-to-es:
	curl -XDELETE 'localhost:9200/oracle-of-beacon?pretty'
	curl -XPUT 'localhost:9200/oracle-of-beacon?pretty' -H 'Content-Type: application/json' -d'{"mappings": {"actors" : {"properties" : {"suggest" : {"type" : "completion" },"name" : {"type": "keyword"}}}}}'
