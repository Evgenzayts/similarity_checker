package org.example;

import com.google.common.hash.Hashing; // Импорты для MurmurHash
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TextSimilarity {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TextSimilarity <file_to_compare> [folder_to_compare]");
            return;
        }

        String filePath = args[0];
        String folderPath = args.length > 1 ? args[1] : "./all";

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Error: File not found.");
            return;
        }

        String referenceText;
        try {
            referenceText = normalizeText(new String(Files.readAllBytes(file.toPath())));
        } catch (IOException e) {
            System.out.println("Error reading reference file.");
            return;
        }

        compareWithFolderUsingMinHash(referenceText, folderPath);
    }

    public static String normalizeText(String text) {
        return text.replaceAll("\\s+", " ").toLowerCase();
    }

    public static int determineNumHashFunctions(int numShingles) {
        return (int) Math.min(200, Math.max(50, Math.sqrt(numShingles) * 2));
    }

    public static void compareWithFolderUsingMinHash(String referenceText, String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Error: Invalid directory.");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("No text files found to compare.");
            return;
        }

        System.out.println("Starting comparison using MinHash similarity...");
        List<Result> results = new ArrayList<>();
        for (File file : files) {
            try {
                String comparedText = normalizeText(new String(Files.readAllBytes(file.toPath())));
                double similarity = calculateMinHashSimilarity(referenceText, comparedText);
                results.add(new Result(file.getName(), similarity));
            } catch (IOException e) {
                System.out.println("Error reading file: " + file.getName());
            }
        }

        results.sort((r1, r2) -> Double.compare(r2.similarity, r1.similarity));
        System.out.println("Results:");
        for (Result result : results) {
            System.out.printf("File: %s, Similarity: %.2f%%%n", result.fileName, result.similarity * 100);
        }
    }

    public static double calculateMinHashSimilarity(String text1, String text2) {
        Set<String> shingles1 = getShingles(text1, 3);
        Set<String> shingles2 = getShingles(text2, 3);

        int numHashFunctions = determineNumHashFunctions(Math.min(shingles1.size(), shingles2.size()));
        if (shingles1.isEmpty() || shingles2.isEmpty()) {
            return 0.0;
        }

        int[] minHashes1 = computeMinHash(shingles1, numHashFunctions);
        int[] minHashes2 = computeMinHash(shingles2, numHashFunctions);

        int matchingHashes = 0;
        for (int i = 0; i < numHashFunctions; i++) {
            if (minHashes1[i] == minHashes2[i]) {
                matchingHashes++;
            }
        }

        return (double) matchingHashes / numHashFunctions;
    }

    public static Set<String> getShingles(String text, int shingleSize) {
        Set<String> shingles = new HashSet<>();
        if (text.length() < shingleSize) return shingles;

        for (int i = 0; i <= text.length() - shingleSize; i++) {
            shingles.add(text.substring(i, i + shingleSize));
        }
        return shingles;
    }

    // Генерация MinHash с использованием MurmurHash
    public static int[] computeMinHash(Set<String> shingles, int numHashFunctions) {
        int[] hashes = new int[numHashFunctions];

        for (int i = 0; i < numHashFunctions; i++) {
            int minHash = Integer.MAX_VALUE;
            for (String s : shingles) {
                // Используем MurmurHash3 с разными начальными значениями (seed)
                int hash = Hashing.murmur3_32().hashString(s + i, StandardCharsets.UTF_8).asInt();
                if (hash < minHash) {
                    minHash = hash;
                }
            }
            hashes[i] = minHash;
        }

        return hashes;
    }

    static class Result {
        String fileName;
        double similarity;

        public Result(String fileName, double similarity) {
            this.fileName = fileName;
            this.similarity = similarity;
        }
    }
}
