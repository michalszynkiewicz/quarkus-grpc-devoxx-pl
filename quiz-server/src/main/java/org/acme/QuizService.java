package org.acme;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.mutiny.core.Vertx;
import org.acme.quiz.grpc.QuizGrpcService;
import org.acme.quiz.grpc.Riddle;
import org.acme.quiz.grpc.Score;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResult;
import org.acme.quiz.grpc.Solution;
import org.acme.quiz.grpc.SolutionResult;
import org.acme.quiz.grpc.UserScore;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@GrpcService
public class QuizService implements QuizGrpcService {
    private final Map<String, String> userTokens = new ConcurrentHashMap<>();
    private final Map<String, String> usersByToken = new ConcurrentHashMap<>();
    private final Set<String> usersWithResponse = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, AtomicInteger> userScores = new ConcurrentHashMap<>();
    private final BroadcastProcessor<Riddle> riddleBroadcast = BroadcastProcessor.create();
    private final BroadcastProcessor<Score> scoreBroadcast = BroadcastProcessor.create();
    private final AtomicReference<RiddleEntity> currentRiddle = new AtomicReference<>();

    private final Random random = new Random();

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Vertx vertx;

    @PostConstruct
    void generateRiddles() {

        Multi.createFrom().ticks()
                .every(Duration.ofSeconds(10))
                .subscribe().with(
                        tick -> {
                            broadcastNextRiddle();

                            Score.Builder scoreBuilder = Score.newBuilder();
                            List<UserScore> userScores = this.userScores.entrySet().stream().map(
                                    entry ->
                                            UserScore.newBuilder()
                                                    .setUser(entry.getKey())
                                                    .setPoints(entry.getValue().get())
                                                    .build()
                            ).collect(Collectors.toList());
                            scoreBuilder.addAllScores(userScores);
                            scoreBroadcast.onNext(scoreBuilder.build());
                        }
                );
    }

    private void broadcastNextRiddle() {
        sessionFactory.openStatelessSession()
                .onItem().invoke(
                        session -> {
                            countRiddles(session)
                                    .onItem().transformToUni(
                                            count -> {
                                                int randomRiddle = random.nextInt(RiddleWithSolution.RIDDLES.length);
                                                System.out.println("selecting riddle " + randomRiddle);
                                                Mutiny.Query<RiddleEntity> query = session.createNativeQuery(
                                                        "select * from riddles order by id limit 1 offset ?1", RiddleEntity.class);
                                                return query
                                                        .setParameter(1, randomRiddle)
                                                        .getSingleResult()
                                                        .onItem().transformToUni(r -> session.fetch(r.answers).replaceWith(r));
                                            }
                                    ).onItem().invoke(currentRiddle::set)
                                    .onItem().transform(RiddleEntity::toGrpcRiddle)
                                    .subscribe().with(riddleBroadcast::onNext);
                        }
                ).subscribe().asCompletionStage();
    }

    private Uni<Integer> countRiddles(Mutiny.StatelessSession session) {
        return session.createQuery("select count(*) from RiddleEntity ", Long.class)
                .getSingleResult().map(Long::intValue);
    }

    @Override
    public Uni<SignUpResult> signUp(SignUpRequest request) {
        String token = RandomStringUtils.randomAlphanumeric(10);
        String name = request.getName();
        if (userTokens.putIfAbsent(name, token) != null) {
            return Uni.createFrom().item(
                    SignUpResult.newBuilder().setResult(SignUpResult.Result.NAME_ALREADY_USED).build()
            );
        }
        usersByToken.put(token, name);
        userScores.put(name, new AtomicInteger(0));
        return Uni.createFrom().item(SignUpResult.newBuilder().setResult(SignUpResult.Result.OKAY).setToken(token).build());
    }

    @Override
    public Uni<SolutionResult> answer(Solution request) {
        SolutionResult.Builder result = SolutionResult.newBuilder();
        RiddleEntity currentRiddle = this.currentRiddle.get();

        if (!usersWithResponse.add(request.getToken())) {
            result.setResult(SolutionResult.Result.DUPLICATE_ANSWER);
        } else if (request.getRiddleId().equals(currentRiddle.id)) {
            if (request.getSolution().equals(currentRiddle.answer)) {
                String userName = usersByToken.get(request.getToken());
                userScores.get(userName).incrementAndGet();
                result.setResult(SolutionResult.Result.OKAY);
            } else {
                result.setResult(SolutionResult.Result.WRONG);
            }
        } else {
            result.setResult(SolutionResult.Result.TIMEOUT);
        }
        return Uni.createFrom().item(result.build());
    }

    @Override
    public Multi<Riddle> getRiddles(Empty request) {
        return Multi.createBy().merging().streams(Multi.createFrom().item(currentRiddle.get().toGrpcRiddle()), riddleBroadcast);
    }

    @Override
    public Multi<Score> watchScore(Empty request) {
        return scoreBroadcast;
    }
}
