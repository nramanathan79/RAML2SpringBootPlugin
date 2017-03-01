FROM DOCKER_BASE_IMAGE_NAME

ARG userid

# Create user
RUN mkdir -p /home/${userid} && groupadd -r ${userid} -g 433 && useradd -u 431 -r -g ${userid} -d /home/${userid} -s /sbin/nologin -c "API User" ${userid}

# Copy the JAR
ADD *.jar /home/${userid}/app.jar

# Switch to user
RUN chown -R ${userid}:${userid} /home/${userid}
USER ${userid}
WORKDIR /home/${userid}

# Execute the Spring Boot JAR
CMD java -Djava.security.egd=file:/dev/./urandom -jar app.jar
