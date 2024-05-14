#!/bin/sh

# ; Weller is like Alfresco out-of-process extensions but 100% Clojure
# ; Copyright (C) 2024 Saidone
# ;
# ; This program is free software: you can redistribute it and/or modify
# ; it under the terms of the GNU General Public License as published by
# ; the Free Software Foundation, either version 3 of the License, or
# ; (at your option) any later version.
# ;
# ; This program is distributed in the hope that it will be useful,
# ; but WITHOUT ANY WARRANTY; without even the implied warranty of
# ; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# ; GNU General Public License for more details.
# ;
# ; You should have received a copy of the GNU General Public License
# ; along with this program.  If not, see <http://www.gnu.org/licenses/>.

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