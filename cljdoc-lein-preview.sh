#!/bin/bash

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

# get project name
PROJECT=$(cat project.clj | grep defproject | cut -d " " -f 2 | cut -d "/" -f 2)
# get group id
GROUP_ID=$(cat project.clj | grep defproject | cut -d " " -f 2 | cut -d "/" -f 1)
# get version
VERSION=$(cat project.clj | grep defproject | cut -d " " -f 3 | tr -d '"')

echo "Generating cljdoc for $PROJECT-$VERSION"

# clean up previous run
sudo rm -rf /tmp/cljdoc
mkdir -p /tmp/cljdoc

# build and install into local repo
echo "Installing $PROJECT-$VERSION jar and pom into local repo"
lein install

# ingest into cljdoc
docker run --rm \
  -v $(pwd):/$PROJECT \
  -v $HOME/.m2:/root/.m2 \
  -v /tmp/cljdoc:/app/data \
  --entrypoint clojure \
  cljdoc/cljdoc -Sforce -M:cli ingest \
    --project $GROUP_ID/$PROJECT \
    --version $VERSION \
    --git /$PROJECT \

# start server
docker run --rm -p 9000:8000 -v /tmp/cljdoc:/app/data -v $HOME/.m2:/root/.m2 cljdoc/cljdoc
