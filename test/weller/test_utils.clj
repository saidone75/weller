;  weller
;  Copyright (C) 2024 Saidone
;
;  This program is free software: you can redistribute it and/or modify
;  it under the terms of the GNU General Public License as published by
;  the Free Software Foundation, either version 3 of the License, or
;  (at your option) any later version.
;
;  This program is distributed in the hope that it will be useful,
;  but WITHOUT ANY WARRANTY; without even the implied warranty of
;  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;  GNU General Public License for more details.
;
;  You should have received a copy of the GNU General Public License
;  along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns weller.test-utils
  (:require [clojure.test :refer :all]
            [cral.api.core.nodes :as nodes]
            [cral.model.alfresco.cm :as cm]
            [cral.model.core :as model]
            [weller.config :as c])
  (:import (java.util UUID)))

(defn get-guest-home
  [ticket]
  (get-in (nodes/get-node ticket "-root-" (model/map->GetNodeQueryParams {:relative-path "/Guest Home"})) [:body :entry :id]))

(defn- create-node
  [name]
  (->> (model/map->CreateNodeBody {:name name :node-type cm/type-content})
       (nodes/create-node (:ticket @c/config) (get-guest-home (:ticket @c/config)))
       (#(get-in % [:body :entry :id]))))

(defn update-node
  [node-id]
  (->> (model/map->UpdateNodeBody {:properties {cm/prop-title (.toString (UUID/randomUUID))}})
       (nodes/update-node (:ticket @c/config) node-id)
       (#(get-in % [:body :entry :id]))))

(defn- delete-node
  [node-id]
  (nodes/delete-node (:ticket @c/config) node-id {:permanent true}))

(defn create-then-delete-node
  [name]
  (->> (create-node name)
       (delete-node)))

(defn create-then-update-then-delete-node
  [name]
  (->> (create-node name)
       (update-node)
       (delete-node)))