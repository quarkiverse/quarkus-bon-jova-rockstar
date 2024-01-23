package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.nio.file.Path;

public class Main {
    // TODO: replace main class with unit tests
    public static void main(String[] args) {
        var rockScoreCalculator = new RockScoreCalculator();
        var rockScore = rockScoreCalculator.calculateRockScore("""
(This program prints the ultimate answer)
Answer is rock it
Shout Answer
                """);

        System.out.print("Rock score: " + rockScore);
    }
}
