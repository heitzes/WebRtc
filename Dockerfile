FROM openjdk:11-jdk
LABEL maintainer="hkyoungjeongi@smilegate.com"
ARG JAR_FILE=build/libs/docker-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} docker-springboot.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/docker-springboot.jar"]