package org.acme;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import org.acme.quiz.grpc.Empty;
import org.acme.quiz.grpc.Question;
import org.acme.quiz.grpc.Quiz;
import org.acme.quiz.grpc.Result;
import org.acme.quiz.grpc.Scores;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResponse;
import org.acme.quiz.grpc.UserScore;
import org.acme.riddle.Riddle;
import org.acme.riddle.RiddleService;
import org.acme.score.ResponseResult;
import org.acme.score.ScoreService;

import javax.inject.Inject;

@GrpcService
public class QuizGrpcService implements Quiz {

    @Inject
    RiddleService riddleService;

    @Inject
    ScoreService scoreService;

    @Override
    public Multi<Question> getQuestions(Empty request) {
        return riddleService.getRiddleStream()
                .onItem().transform(this::riddleToQuestion);
    }

    @Override
    public Uni<Result> respond(org.acme.quiz.grpc.Answer request) {
        return scoreService.addResponse(request.getUser(), request.getQuestion(), request.getText())
                .onItem().transform(
                        status ->
                                Result.newBuilder().setStatus(toGrpcStatus(status)).build()
                );
    }

    @Override
    public Multi<Scores> watchScore(Empty request) {
        return scoreService.getScoreBroadcast()
                .onItem().transform(pointsByUser -> {
                    Scores.Builder scores = Scores.newBuilder();
                    pointsByUser
                            .forEach((user, points) -> scores.addResults(UserScore.newBuilder().setUser(user).setPoints(points).build()));
                    return scores.build();
                });
    }

    @Override
    public Uni<SignUpResponse> signUp(SignUpRequest request) {
        return scoreService.addUser(request.getName())
                .map(result -> result ? SignUpResponse.Status.OKAY : SignUpResponse.Status.NAME_TAKEN)
                .onItem().transform(SignUpResponse.newBuilder()::setStatus)
                .onItem().transform(SignUpResponse.Builder::build);
    }

    private Result.Status toGrpcStatus(ResponseResult status) {
        switch (status) {
            case CORRECT:
                return Result.Status.CORRECT;
            case WRONG:
                return Result.Status.WRONG;
            case DUPLICATE_ANSWER:
                return Result.Status.DUPLICATE_ANSWER;
            case TIMEOUT:
                return Result.Status.TIMEOUT;
        }
        return null;
    }

    public Question riddleToQuestion(Riddle riddle) {
        return Question.newBuilder()
                .setText(riddle.text)
                .addAllAnswers(riddle.options)
                .build();
    }

}
