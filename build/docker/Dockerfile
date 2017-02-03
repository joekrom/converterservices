FROM debian
RUN apt-get update && apt-get install -y imagemagick openjdk-8-jre libimage-exiftool-perl
COPY build/libs/converterservices-all-1.0-SNAPSHOT.jar /opt/
EXPOSE 4567
CMD ["java", "-jar", "/opt/converterservices-all-1.0-SNAPSHOT.jar"]