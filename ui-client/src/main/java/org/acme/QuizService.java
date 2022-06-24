package org.acme;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.dto.JoinDto;
import org.acme.dto.JoinResponseDto;
import org.acme.dto.Question;
import org.acme.dto.ResponseDto;
import org.acme.dto.Score;
import org.acme.quiz.grpc.Answer;
import org.acme.quiz.grpc.Empty;
import org.acme.quiz.grpc.Quiz;
import org.acme.quiz.grpc.Result;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResponse;

import javax.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class QuizService {

    @GrpcClient
    Quiz quizClient;

    Multi<Question> getRiddles() {
        return quizClient.getQuestions(Empty.getDefaultInstance())
                .map(this::grpcToDtoRiddle);
    }

    private Question grpcToDtoRiddle(org.acme.quiz.grpc.Question riddle) {
        Question question = new Question();
        question.text = riddle.getText();
        question.answers = riddle.getAnswersList();
        return question;
    }

    public Uni<org.acme.dto.SolutionResult> checkResponse(ResponseDto response) {
        Answer solution = Answer.newBuilder()
                .setText(response.answer)
                .setQuestion(response.question)
                .setUser(response.user)
                .build();
        return quizClient.respond(solution).map(Result::getStatus)
                .map(r -> r == Result.Status.CORRECT ? org.acme.dto.SolutionResult.okay
                        : r == Result.Status.TIMEOUT ? org.acme.dto.SolutionResult.timeout : org.acme.dto.SolutionResult.wrong);
    }

    public Uni<JoinResponseDto> join(JoinDto joinDto) {
        return quizClient.signUp(SignUpRequest.newBuilder()
                        .setName(joinDto.name)
                        .build())
                .onItem().transform(r -> {
                    JoinResponseDto response = new JoinResponseDto();
                    if (r.getStatus() == SignUpResponse.Status.NAME_TAKEN) {
                        response.result = JoinResponseDto.Result.NAME_ALREADY_USED;
                    } else {
                        response.result = JoinResponseDto.Result.OKAY;
                        response.user = joinDto.name;
                    }
                    return response;
                });
    }

    public Multi<List<Score>> getScores() {
        return quizClient.watchScore(Empty.getDefaultInstance())
                .onItem().transform(
                        score ->
                                score.getResultsList().stream().map(us -> new Score(us.getUser(), us.getPoints()))
                                        .sorted(Comparator.comparing(s -> s.score, Comparator.reverseOrder()))
                                        .limit(20)
                                        .collect(Collectors.toList())
                );
    }
}
