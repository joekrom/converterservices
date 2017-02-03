FROM openjdk:8-jre
RUN apt-get update && apt-get install -y imagemagick libimage-exiftool-perl
RUN mkdir /root/.converterservices && mkdir /root/.converterservices/temp
COPY converterservices-all-1.0-SNAPSHOT.jar /opt/
EXPOSE 4567
WORKDIR /root
CMD ["java", "-jar", "/opt/converterservices-all-1.0-SNAPSHOT.jar"]