# Efficient Communication with Quarkus and gRPC Demo

This repository contains the slides and the source code for the Devoxx PL 2022 talk.


The structure of the repo:
- `quiz-server` is the quiz server, it was demoed on the presentation,
- `ui-client` consumes the gRPC services exposed by the `quiz-server` and exposes a web page that lets you play the quiz
- `console-client` is a command line client for the `quiz-server`


This project is based on the demo we prepared for a talk on JNation https://github.com/michalszynkiewicz/jnation-grpc-demo