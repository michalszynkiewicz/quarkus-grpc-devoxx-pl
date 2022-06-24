package org.acme;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.acme.quiz.grpc.Quiz;
import org.acme.quiz.grpc.SignUpRequest;
import org.acme.quiz.grpc.SignUpResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@QuarkusTest
public class QuizGrpcServiceTest {

    @GrpcClient
    Quiz quizClient;

    @Test
    void shouldRegisterUser() {
        SignUpRequest michal = SignUpRequest.newBuilder().setName("Michał").build();
        SignUpResponse response = quizClient.signUp(michal).await().atMost(Duration.ofSeconds(5));

        Assertions.assertThat(response.getStatus()).isEqualTo(SignUpResponse.Status.OKAY);
        response = quizClient.signUp(michal).await().atMost(Duration.ofSeconds(5));

        Assertions.assertThat(response.getStatus()).isEqualTo(SignUpResponse.Status.NAME_TAKEN);
    }
}
