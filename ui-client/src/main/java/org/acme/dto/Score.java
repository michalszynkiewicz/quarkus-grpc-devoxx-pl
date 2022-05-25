package org.acme.dto;

public class Score {
    public String name;

    public Score(String name, Integer score) {
        this.name = name;
        this.score = score;
    }

    public Score() {
    }

    public Integer score;
}
