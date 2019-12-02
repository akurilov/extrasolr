FROM adoptopenjdk:13
ADD src ./src/
ADD gradle ./gradle/
ADD build.gradle ./build.gradle.kts
ADD gradlew ./gradlew
RUN ./gradlew clean jar
RUN ls -l build/libs

FROM #TODO
COPY --from=0 /build/libs/extrasolr.jar /opt/extrasolr/extrasolr.jar
ADD ci/docker/entrypoint.sh /opt/extrasolr/entrypoint.sh
ENTRYPOINT /opt/extrasolr/entrypoint.sh
