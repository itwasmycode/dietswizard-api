FROM hseeberger/scala-sbt:graalvm-ce-19.3.0-java11_1.3.7_2.13.1 as builder
COPY . /lambda/src/
WORKDIR /lambda/src/
RUN sbt assembly

FROM public.ecr.aws/lambda/java:11
COPY --from=builder /lambda/src/target/function.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["LambdaHandler::handleRequest"]