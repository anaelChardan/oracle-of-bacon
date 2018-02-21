package com.serli.oracle.of.bacon.loader.elasticsearch;

import com.serli.oracle.of.bacon.repository.ElasticSearchRepository;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.serli.oracle.of.bacon.utils.CombinationBuilder.getAllCombinations;

public class CompletionLoader {
    private static AtomicInteger count = new AtomicInteger(0);
    private static AtomicInteger maxCount = new AtomicInteger(0);
    private static final String ES_INDEX = "oracle-of-beacon";
    private static final String ES_TYPE = "actors";
    private static final long MB_SIZE = 1048576;
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchRepository.class);

    public static void main(String[] args) throws IOException {
        RestHighLevelClient client = ElasticSearchRepository.createClient();

        if (args.length != 1) {
            System.err.println("Expecting 1 arguments, actual : " + args.length);
            System.err.println("Usage : completion-loader <actors file path>");
            System.exit(-1);
        }

        String inputFilePath = args[0];

        LinkedList<BulkRequest> requests = new LinkedList<>();
        requests.add(new BulkRequest());

        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(inputFilePath))) {
            bufferedReader
                    .lines()
                    .forEach(line -> {
                        if ("name:ID".equals(line)) {
                            return;
                        }

                        String lineWithoutQuotes = line.substring(1, line.length() - 1);
                        // If the BulkRequest size is greater or equals than 9MB.
                        // Creates a new BulkRequest.
                        if (requests.peekLast().estimatedSizeInBytes() > 9 * MB_SIZE) {
                            requests.add(new BulkRequest());
                        }

                        Map<String, Object> jsonMap = new HashMap<>();
                        jsonMap.put("name", lineWithoutQuotes);
                        String[] strings = lineWithoutQuotes.trim().split("\\s+");
                        jsonMap.put("suggest", getAllCombinations(strings));

                        BulkRequest currentRequest = requests.peekLast(); //takes the last request manipulated.
                        currentRequest.add(new IndexRequest(ES_INDEX, ES_TYPE).source(jsonMap));
                        maxCount.incrementAndGet();
                    });
        }

        makeAllRequestsAsynch(client, requests);
    }

    /**
     * Makes all BulkRequest asynchronously. A BulkRequest is sent after the callback of the previous one (recursive function).
     *
     * @param client   client of elasticsearch db.
     * @param requests list of all BulkRequest (A BulkRequest data lenght is lte to 10MB).
     *                 This is a LinkedList in order to poll it from BulkRequest.
     * @throws IOException
     */
    private static void makeAllRequestsAsynch(RestHighLevelClient client, LinkedList<BulkRequest> requests) throws IOException {

        if (requests.size() == 0) {
            client.close();
            logger.debug("Inserted {} actors of {}", count.get(), maxCount.get());
            return;
        }

        final BulkRequest currentRequest = requests.pollFirst();

        client.bulkAsync(currentRequest, new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                count.addAndGet(bulkItemResponses.getItems().length); //increments the count number
                try {
                    logger.debug("Inserted {} actors of {}", count.get(), maxCount.get());
                    makeAllRequestsAsynch(client, requests); //call the same function with the tail of the list
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Exception e) {
                try {
                    client.close();
                    return;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }
}
