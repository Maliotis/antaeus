FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y sqlite3

COPY . /anteus
WORKDIR /anteus

EXPOSE 7000

RUN useradd -u 8877 petros
#USER petros
#WORKDIR /home/petros
#ENV PATH="/home/petros/.local/bin:${PATH}"
#COPY --chown=petros:petros . .

# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run
