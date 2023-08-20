FROM hseeberger/scala-sbt:graalvm-ce-19.3.0-java11_1.3.7_2.13.1 as builder
COPY . /lambda/src/
WORKDIR /lambda/src/
RUN sbt assembly


# Runner Stage Local Env Starts
# FROM amazoncorretto:11
# COPY --from=builder /lambda/src/target/function.jar /var/runtime/lib/

# Download and set up the Lambda RIE
# RUN curl -Lo /usr/local/bin/aws-lambda-rie https://github.com/aws/aws-lambda-runtime-interface-emulator/releases/latest/download/aws-lambda-rie \
#    && chmod +x /usr/local/bin/aws-lambda-rie

# Define the handler
# ENV AWS_LAMBDA_FUNCTION_HANDLER="LambdaHandler::handleRequest"

# Custom entrypoint
# ENTRYPOINT [ "/usr/local/bin/aws-lambda-rie" ]
# CMD [ "java", "-cp", "/var/runtime/lib/*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda", "LambdaHandler::handleRequest" ]
# Runner Stage Local Env Ends

FROM public.ecr.aws/lambda/java:11
COPY --from=builder /lambda/src/target/function.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["LambdaHandler::handleRequest"]