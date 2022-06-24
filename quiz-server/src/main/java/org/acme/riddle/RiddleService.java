package org.acme.riddle;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.vertx.core.Vertx;
import org.acme.score.ScoreService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;

@ApplicationScoped
public class RiddleService {
    private static final Riddle ENDING_RIDDLE = new Riddle("That's all, thanks for playing!", "");

    @ConfigProperty(name = "quiz.delay", defaultValue = "2s")
    Duration delay;

    @Inject
    Vertx vertx;

    @Inject
    RiddleStorage riddleStorage;

    @Inject
    ScoreService scoreService;

    private final UnicastProcessor<Riddle> questionBroadcast = UnicastProcessor.create();
    private final Multi<Riddle> riddles = Multi.createBy().replaying().upTo(1).ofMulti(questionBroadcast)
            .broadcast().toAllSubscribers();

    public void startQuiz() {
        scoreService.clearPoints();
        broadcastQuestion(0);
    }

    public void endQuiz() {
        questionBroadcast.onNext(ENDING_RIDDLE);
        scoreService.replaceRiddle(null);
    }

    void broadcastQuestion(int i) {
        Riddle riddle = riddleStorage.getRiddle(i);
        if (riddle == null) {
            endQuiz();
        } else {
            questionBroadcast.onNext(riddle);
            scoreService.replaceRiddle(riddle);
            vertx.setTimer(delay.toMillis(), ignored -> broadcastQuestion(i + 1));
        }
    }

    /**
     * get stream of riddles
     * @return stream of riddles
     */
    public Multi<Riddle> getRiddleStream() {
        return riddles;
    }
}
