#FROM openjdk:8
FROM amazonlinux:latest

RUN yum -y install unzip aws-cli java-1.8.0

COPY au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar /usr/local/bin/app.jar
ADD au_runner.sh /usr/local/bin/au_runner.sh
RUN chmod +x /usr/local/bin/au_runner.sh
WORKDIR /
ENTRYPOINT ["/usr/local/bin/au_runner.sh"]
#CMD ["java", "-Xmx2g", "-Djava.awt.headless=true", "-jar", "app.jar"]
