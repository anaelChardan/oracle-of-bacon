package com.serli.oracle.of.bacon.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see https://stackoverflow.com/questions/5162254/all-possible-combinations-of-an-array
 */
public final class CombinationBuilder {
    public static List<String> getAllCombinations(String[] stringsToSplit) {
        List<List<String>> powerSet = new LinkedList<List<String>>();

        for (int i = 1; i <= stringsToSplit.length; i++)
            powerSet.addAll(combination(Arrays.asList(stringsToSplit), i));

        return powerSet.stream().map(e -> String.join(" ", e)).collect(Collectors.toList());
    }

    public static <T> List<List<T>> combination(List<T> values, int size) {
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
