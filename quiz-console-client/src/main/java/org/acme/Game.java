package org.acme;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.acme.quiz.grpc.QuizGrpcService;
import org.acme.quiz.grpc.Riddle;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResult;
import org.acme.quiz.grpc.Solution;
import org.acme.quiz.grpc.SolutionResult;
import org.acme.quiz.grpc.UserScore;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@QuarkusMain
public class Game implements QuarkusApplication {

    @GrpcClient
    QuizGrpcService quizClient;

    volatile Riddle currentRiddle;

    @Override
    public int run(String... args) {
        if (args.length == 0) {
            Console.red("Type your name...");
            return 1;
        }

        String token = signUp(args[0]);

        quizClient.getRiddles(Empty.getDefaultInstance())
                .subscribe().with(riddle -> {
                    Console.cyan("Riddle: " + riddle.getText());
                    currentRiddle = riddle;
                });
        quizClient.watchScore(Empty.getDefaultInstance())
                .subscribe().with(result -> {
                    List<UserScore> results = new ArrayList<>(result.getScoresList());
                    results.sort(Comparator.comparing(UserScore::getPoints));
                    Console.white("Results: ");
                    Console.white("======");
                    for (UserScore userScore : results) {
                        Console.white("%s: %s", userScore.getUser(), userScore.getPoints());
                    }
                    Console.white("======");

                });

        while (true) {
            String resultStr = "";
            resultStr = System.console().readLine().trim();
            SolutionResult result = quizClient.answer(
                            Solution.newBuilder().setSolution(resultStr).setToken(token).setRiddleId(currentRiddle.getRiddleId()).build()
                    )
                    .await().atMost(Duration.ofSeconds(5));

            switch (result.getResult()) {
                case OKAY:
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

    private String signUp(String name) {
        SignUpResult signUpResult = quizClient.signUp(SignUpRequest.newBuilder().setName(name).build())
                .await().atMost(Duration.ofSeconds(20));
        switch (signUpResult.getResult()) {
            case OKAY:
                return signUpResult.getToken();
            case NAME_ALREADY_USED:
                throw new RuntimeException("Name already used, please use a different one");
            default:
                throw new RuntimeException("Undefined problem");
        }
    }

}
