package org.acme;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.acme.quiz.grpc.Answer;
import org.acme.quiz.grpc.Empty;
import org.acme.quiz.grpc.Question;
import org.acme.quiz.grpc.Quiz;
import org.acme.quiz.grpc.Result;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResponse;
import org.acme.quiz.grpc.UserScore;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@QuarkusMain
public class Game implements QuarkusApplication {

    @GrpcClient
    Quiz quizClient;

    volatile Question currentQuestion;

    @Override
    public int run(String... args) {
        if (args.length == 0) {
            Console.red("Type your name...");
            return 1;
        }

        String userName = args[0];
        signUp(userName);

        quizClient.getQuestions(Empty.getDefaultInstance())
                .subscribe().with(riddle -> {
                    Console.cyan("Riddle: " + riddle.getText());
                    Console.cyan("Responses: " + String.join(", ", riddle.getAnswersList()));
                    currentQuestion = riddle;
                });
        quizClient.watchScore(Empty.getDefaultInstance())
                .subscribe().with(result -> {
                    List<UserScore> results = new ArrayList<>(result.getResultsList());
                    results.sort(Comparator.comparing(UserScore::getPoints));
                    Console.white("Results: ");
                    Console.white("======");
                    for (UserScore userScore : results) {
                        Console.white("%s: %s", userScore.getUser(), userScore.getPoints());
                    }
                    Console.white("======");

                });

        while (true) {
            String resultStr = System.console().readLine().trim();
            System.out.println("answering to " + currentQuestion.getAnswersList() + "; " + currentQuestion.getText());
            Result response = quizClient.respond(
                            Answer.newBuilder().setText(resultStr).setUser(userName).setQuestion(currentQuestion.getText()).build()
                    )
                    .await().atMost(Duration.ofSeconds(5));

            switch (response.getStatus()) {
                case CORRECT:
                    Console.green("Correct!");
                    Console.white("Please wait for another riddle");
                    break;
                case WRONG:
                    Console.red("That's not the correct answer.");
                    break;
                case TIMEOUT:
                    Console.yellow("The time was up.");
                    break;
                case DUPLICATE_ANSWER:
                    Console.yellow("You have already made an attempt to answer.");
                    break;
                default:
                    break;
            }
        }
    }

    private void signUp(String name) {
        SignUpResponse signUpResult = quizClient.signUp(SignUpRequest.newBuilder().setName(name).build())
                .await().atMost(Duration.ofSeconds(20));
        switch (signUpResult.getStatus()) {
            case OKAY:
                break;
            case NAME_TAKEN:
                throw new RuntimeException("Name already used, please use a different one");
            default:
                throw new RuntimeException("Undefined problem");
        }
    }

}
