# Use an OpenJDK base image
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the application's JAR file into the container
COPY target/stellar-streams-app.jar /app/stellar-streams-app.jar

# Copy the topic creation script into the container

# Set the entry point to run the application
ENTRYPOINT ["java", "-Xmx7500M", "-jar"]
CMD ["/app/stellar-streams-app.jar"]
