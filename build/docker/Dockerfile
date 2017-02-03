FROM openjdk:8-jre
RUN apt-get update && apt-get install -y imagemagick libimage-exiftool-perl
COPY ../libs/converterservices-all-1.0-SNAPSHOT.jar /opt/
EXPOSE 4567
CMD ["java", "-jar", "/opt/converterservices-all-1.0-SNAPSHOT.jar"]