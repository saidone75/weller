#!/usr/bin/env bash
#
# Weller - Here's your servant, Sir. Out o' the process.
# Copyright (C) 2023-2026 Saidone
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

set -euo pipefail
cd "$(dirname "$0")"

IFS= read -r -p "Clojars username: " clojars_username
IFS= read -r -s -p "Clojars deploy token: " clojars_token
printf '\n'

if [[ -z "$clojars_username" || -z "$clojars_token" ]]; then
  printf 'Username and deploy token are required.\n' >&2
  exit 1
fi

CLOJARS_USERNAME="$clojars_username" CLOJARS_PASSWORD="$clojars_token" \
  clojure -T:build deploy
