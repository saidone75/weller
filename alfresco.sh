#!/bin/sh

export COMPOSE_FILE_PATH="${PWD}/docker/docker-compose.yml"

start() {
    docker volume create weller-acs-volume
    docker volume create weller-db-volume
    docker volume create weller-ass-volume
    docker-compose -f "$COMPOSE_FILE_PATH" --env-file ./docker/.env up --build -d
}

down() {
    if [ -f "$COMPOSE_FILE_PATH" ]; then
        docker-compose -f "$COMPOSE_FILE_PATH" --env-file ./docker/.env down
    fi
}

purge() {
    docker volume rm -f weller-acs-volume
    docker volume rm -f weller-db-volume
    docker volume rm -f weller-ass-volume
}

tail() {
    docker-compose -f "$COMPOSE_FILE_PATH" --env-file ./docker/.env logs -f
}

tail_all() {
    docker-compose -f "$COMPOSE_FILE_PATH" --env-file ./docker/.env logs --tail="all"
}

case "$1" in
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  purge)
    down
    purge
    ;;
  tail)
    tail
    ;;
  *)
    echo "Usage: $0 {start|stop|purge|tail}"
esac