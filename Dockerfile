FROM amazoncorretto:17.0.6
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/libs/filesharing-backend.jar /app/filesharing-backend.jar
ENTRYPOINT ["java","-jar","/app/filesharing-backend.jar"]
