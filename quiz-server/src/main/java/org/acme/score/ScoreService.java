package org.acme.score;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.acme.riddle.Riddle;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ScoreService {

    private final Map<String, Integer> pointsByUser = new ConcurrentHashMap<>();

    private final BroadcastProcessor<Map<String, Integer>> scoreBroadcast = BroadcastProcessor.create();

    private final Set<String> usersWithAnswer = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final AtomicReference<Riddle> currentRiddle = new AtomicReference<>();

    /**
     * get a stream of user scores
     * @return user scores stream
     */
    public Multi<Map<String, Integer>> getScoreBroadcast() {
        return scoreBroadcast;
    }

    /**
     * add a user to keep score for
     * @param name username
     * @return true if a user was added, false if a user with the given name was already defined
     */
    public Uni<Boolean> addUser(String name) {
        if (pointsByUser.putIfAbsent(name, 0) != null) {
            return Uni.createFrom().item(false);
        } else {
            return Uni.createFrom().item(true);
        }
    }

    /**
     *
     * @param user username
     * @param question the text of the question
     * @param text the text of the answer
     * @return if the answer was good
     */
    public Uni<ResponseResult> addResponse(String user, String question, String text) {
        Riddle riddle = currentRiddle.get();
        ResponseResult result;
        if (riddle == null || !riddle.text.equals(question)) {
            result = ResponseResult.TIMEOUT;
        } else {
            if (!usersWithAnswer.add(user)) {
                result = ResponseResult.DUPLICATE_ANSWER;
            } else if (!riddle.answer.equals(text)) {
                result = ResponseResult.WRONG;
            } else {
                result = ResponseResult.CORRECT;
                pointsByUser.put(user, pointsByUser.get(user) + 1);
            }
        }
        return Uni.createFrom().item(result);
    }

    public void replaceRiddle(Riddle riddle) {
        currentRiddle.set(riddle);
        broadcastResults();
        usersWithAnswer.clear();
    }

    public void clearPoints() {
        pointsByUser.replaceAll((k, v) -> 0);
    }

    private void broadcastResults() {
        scoreBroadcast.onNext(pointsByUser);
    }
}
