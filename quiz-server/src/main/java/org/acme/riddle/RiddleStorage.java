package org.acme.riddle;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RiddleStorage {

    @ConfigProperty(name = "quiz.prod-questions", defaultValue = "false")
    boolean useProdQuestions;

    private final List<Riddle> riddles = new ArrayList<>();

    @PostConstruct
    void setUp() {
        if (useProdQuestions) {
            fillProdQuestions();
        } else {
            riddles.add(new Riddle("How much is 2+3?", "5", "1", "6"));
            riddles.add(new Riddle("What is #000000 in RGB", "black", "white", "yellow"));
            riddles.add(new Riddle("How tall is Jack Reacher", "195", "180", "200"));
        }
    }

    public Riddle getRiddle(int riddleNumber) {
        return riddleNumber < riddles.size() ? riddles.get(riddleNumber) : null;
    }











    private void fillProdQuestions() {
        riddles.add(new Riddle("Can Quarkus do gRPC?", "Yes", "No"));
        riddles.add(new Riddle("What is the major part of the Quarkus version?", "2", "0", "1", "4"));
        riddles.add(new Riddle("How many Quarkus contributors are there?", "500-1000", "1-10", "11-100", "101-500"));
        riddles.add(new Riddle("Does Quarkus recompile proto files automatically in Dev Mode?", "Yes", "No"));
    }

}
