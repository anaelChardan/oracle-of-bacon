package com.serli.oracle.of.bacon.loader.elasticsearch;

import com.serli.oracle.of.bacon.repository.ElasticSearchRepository;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CompletionLoader {
    private static AtomicInteger count = new AtomicInteger(0);
    private static final String ES_INDEX = "oracle-of-beacon";
    private static final String ES_TYPE = "actors";
    private static final long MB_SIZE = 1048576;

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
            System.out.println("Inserted total of " + count.get() + " actors");
            return;
        }

        final BulkRequest currentRequest = requests.pollFirst();

        client.bulkAsync(currentRequest, new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                count.addAndGet(bulkItemResponses.getItems().length); //increments the count number
                try {
                    System.out.println("C'est inséré");
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

    //Ce qui est en dessous provient de @link{https://stackoverflow.com/questions/5162254/all-possible-combinations-of-an-array}

    private static List<String> getAllCombinations(String[] stringsToSplit) {
        List<List<String>> powerSet = new LinkedList<List<String>>();

        for (int i = 1; i <= stringsToSplit.length; i++)
            powerSet.addAll(combination(Arrays.asList(stringsToSplit), i));

        return powerSet.stream().map(e -> String.join(" ", e)).collect(Collectors.toList());
    }

    private static <T> List<List<T>> combination(List<T> values, int size) {

        if (0 == size) {
            return Collections.singletonList(Collections.<T> emptyList());
        }

        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> combination = new LinkedList<List<T>>();

        T actual = values.iterator().next();

        List<T> subSet = new LinkedList<T>(values);
        subSet.remove(actual);

        List<List<T>> subSetCombination = combination(subSet, size - 1);

        for (List<T> set : subSetCombination) {
            List<T> newSet = new LinkedList<T>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }

        combination.addAll(combination(subSet, size));

        return combination;
    }

}
