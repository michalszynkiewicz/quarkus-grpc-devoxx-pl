package org.acme;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiBroadcast;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.acme.dto.Question;
import org.acme.dto.JoinDto;
import org.acme.dto.JoinResponseDto;
import org.acme.dto.ResponseDto;
import org.acme.dto.Score;
import org.acme.dto.SolutionResult;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static java.util.Arrays.asList;

@Path("/game")
public class GameResource {

    Multi<Question> riddleBroadcast;
    Multi<List<Score>> scoresBroadcast;

    @Inject
    QuizService quizService;

    volatile Question currentQuestion;

    @PostConstruct
    void initializeQuestionStream() {
        riddleBroadcast = quizService.getRiddles().broadcast().toAllSubscribers()
                .onItem().invoke(question -> currentQuestion = question);
        scoresBroadcast = quizService.getScores().broadcast().toAllSubscribers();
    }

    @POST
    @Path("/join")
    public Uni<JoinResponseDto> join(JoinDto joinDto) {
        return quizService.join(joinDto);
    }

    @POST
    @Path("/respond")
    public Uni<SolutionResult> respond(ResponseDto response) {
        return quizService.checkResponse(response);
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/questions")
    public Multi<Question> getQuestions() {
        return Multi.createBy().merging().streams(Multi.createFrom().item(currentQuestion), riddleBroadcast);
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/scores")
    public Multi<List<Score>> getScores() {
        return scoresBroadcast;
    }

}