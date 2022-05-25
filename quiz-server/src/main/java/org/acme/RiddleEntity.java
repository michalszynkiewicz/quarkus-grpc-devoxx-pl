package org.acme;


import org.acme.quiz.grpc.Riddle;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "riddles")
public class RiddleEntity {
    @Id
    public String id;
    public String question;
    public String answer;
    @ElementCollection
    @CollectionTable(name = "riddle_answers")
    public List<String> answers;

    public Riddle toGrpcRiddle() {
        Riddle.Builder riddle = Riddle.newBuilder().setRiddleId(id)
                .setText(question);
        riddle.addAllResponses(answers);
        return riddle.build();
    }
}
