mvn clean install -DskipTests
cp target/*-jar-with-dependencies.jar ./
rsync