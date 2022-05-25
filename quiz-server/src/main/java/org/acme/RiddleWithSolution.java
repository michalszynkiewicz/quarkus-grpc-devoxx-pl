package org.acme;

import org.acme.quiz.grpc.Riddle;

import java.util.List;

import static java.util.Arrays.asList;

public class RiddleWithSolution {
    public final String riddleId;
    public final String text;
    public final String solution;
    public final List<String> responses;

    public RiddleWithSolution(String riddleId, String text, String solution, String... responses) {
        this.riddleId = riddleId;
        this.text = text;
        this.solution = solution;
        this.responses = asList(responses);
    }

    static final RiddleWithSolution[] RIDDLES = new RiddleWithSolution[] {
            new RiddleWithSolution("1", "Can Quarkus do gRPC", "yes", "yes", "no"),
            new RiddleWithSolution("2", "Can Quarkus do gRPC", "yes", "yes", "no"),
            new RiddleWithSolution("3", "Can Quarkus do gRPC", "yes", "yes", "no"),
            new RiddleWithSolution("4", "Can Quarkus do gRPC", "yes", "yes", "no"),
            new RiddleWithSolution("5", "Can Quarkus do gRPC", "yes", "yes", "no"),
            new RiddleWithSolution("6", "Can Quarkus do gRPC", "yes", "yes", "no"),
    };

    public Riddle toGrpcRiddle() {
        Riddle.Builder riddle = Riddle.newBuilder().setRiddleId(riddleId)
                .setText(text);
        riddle.addAllResponses(responses);
        return riddle.build();
    }
}
