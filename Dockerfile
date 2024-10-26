# Use an OpenJDK base image
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the application's JAR file into the container
COPY target/stellar-streams-app.jar /app/stellar-streams-app.jar

# Set the entry point to run the application
ENTRYPOINT ["java", "-Xmx7500M", "-jar"]
CMD ["/app/stellar-streams-app.jar"]

## Copy the topic creation script into the container
#COPY conf/create-topics.sh /app/create-topics.sh
#RUN chmod +x /app/create-topics.sh
#RUN echo '#!/bin/bash\n\
#bash /app/create-topics.sh\n\
#echo "Starting the application..."\n\
#java -Xmx7500M -jar /app/stellar-streams-app.jar\n\
#' > /app/start.sh && chmod +x /app/start.sh
## Set the entry point to run the startup script
#ENTRYPOINT ["/app/start.sh"]


