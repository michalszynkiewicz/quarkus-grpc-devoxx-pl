package org.acme;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.acme.dto.JoinDto;
import org.acme.dto.JoinResponseDto;
import org.acme.dto.Question;
import org.acme.dto.ResponseDto;
import org.acme.dto.Score;
import org.acme.quiz.grpc.QuizGrpcService;
import org.acme.quiz.grpc.Riddle;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResult;
import org.acme.quiz.grpc.Solution;
import org.acme.quiz.grpc.SolutionResult;

import javax.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class QuizService {

    @GrpcClient
    QuizGrpcService quizClient;

    Multi<Question> getRiddles() {
        return quizClient.getRiddles(Empty.getDefaultInstance())
                .map(this::grpcToDtoRiddle);
    }

    private Question grpcToDtoRiddle(Riddle riddle) {
        Question question = new Question();
        question.riddleId = riddle.getRiddleId();
        question.text = riddle.getText();
        question.answers = riddle.getResponsesList();
        return question;
    }

    public Uni<org.acme.dto.SolutionResult> checkResponse(ResponseDto response) {
        Solution solution = Solution.newBuilder()
                .setSolution(response.answer)
                .setRiddleId(response.riddleId)
                .setToken(response.token)
                .build();
        return quizClient.answer(solution).map(SolutionResult::getResult)
                .map(r -> r == SolutionResult.Result.OKAY ? org.acme.dto.SolutionResult.okay
                        : r == SolutionResult.Result.TIMEOUT ? org.acme.dto.SolutionResult.timeout : org.acme.dto.SolutionResult.wrong);
    }

    public Uni<JoinResponseDto> join(JoinDto joinDto) {
        return quizClient.signUp(SignUpRequest.newBuilder()
                        .setName(joinDto.name)
                .build())
                .onItem().transform(r -> {
                    JoinResponseDto response = new JoinResponseDto();
                    if (r.getResult() == SignUpResult.Result.NAME_ALREADY_USED) {
                        response.result = JoinResponseDto.Result.NAME_ALREADY_USED;
                    } else {
                        response.result = JoinResponseDto.Result.OKAY;
                        response.token = r.getToken();
                    }
                    return response;
                });
    }

    public Multi<List<Score>> getScores() {
        return quizClient.watchScore(Empty.getDefaultInstance())
                .onItem().transform(
                    score ->
                        score.getScoresList().stream().map(us -> new Score(us.getUser(), us.getPoints()))
                                .sorted(Comparator.comparing(s -> s.score, Comparator.reverseOrder()))
                                .collect(Collectors.toList())
                );
    }
}
