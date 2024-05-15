;  Weller is like Alfresco out-of-process extensions but 100% Clojure
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

(defn- get-guest-home
  []
  (get-in (nodes/get-node (:ticket @c/config) "-root-" (model/map->GetNodeQueryParams {:relative-path "/Guest Home"})) [:body :entry :id]))

(defn- create-folder
  []
  (->> (model/map->CreateNodeBody {:name (.toString (UUID/randomUUID)) :node-type cm/type-folder})
       (nodes/create-node (:ticket @c/config) (get-guest-home))
       (#(get-in % [:body :entry :id]))))

(defn- create-node
  ([]
   (create-node (.toString (UUID/randomUUID))))
  ([name]
   (create-node name (get-guest-home)))
  ([name parent-id]
   (->> (model/map->CreateNodeBody {:name name :node-type cm/type-content})
        (nodes/create-node (:ticket @c/config) parent-id)
        (#(get-in % [:body :entry :id])))))

(defn- update-node
  [node-id]
  (->> (model/map->UpdateNodeBody {:properties {cm/prop-title (.toString (UUID/randomUUID))}})
       (nodes/update-node (:ticket @c/config) node-id)
       (#(get-in % [:body :entry :id]))))

(defn- delete-node
  [node-id]
  (nodes/delete-node (:ticket @c/config) node-id {:permanent true}))

(defn create-then-update-then-delete-node
  ([]
   (create-then-update-then-delete-node (.toString (UUID/randomUUID))))
  ([name]
   (->> (create-node name)
        (update-node)
        (delete-node))))

(defn create-then-delete-child-assoc
  []
  (let [parent-node-id (create-folder)
        child-node-id (create-node)]
    (->> [(model/map->CreateSecondaryChildBody {:child-id child-node-id :assoc-type cm/assoc-contains})]
         (nodes/create-secondary-child (:ticket @c/config) parent-node-id))
    (nodes/delete-secondary-child (:ticket @c/config) parent-node-id child-node-id)
    (delete-node child-node-id)
    (delete-node parent-node-id)))

(defn create-then-delete-peer-assoc
  []
  (let [created-node-id (create-node)]
    (->> (model/map->CreateNodeAssocsBody {:target-id created-node-id :assoc-type cm/assoc-contains})
         (nodes/create-node-assocs (:ticket @c/config) (get-guest-home)))
    (nodes/delete-node-assocs (:ticket @c/config) (get-guest-home) created-node-id)
    (delete-node created-node-id)))

(defn add-aspect
  [aspect-name]
  (let [created-node-id (create-node)
        aspect-names (get-in (nodes/get-node (:ticket @c/config) created-node-id) [:body :entry :aspect-names])]
    (->> (model/map->UpdateNodeBody {:aspect-names (conj aspect-names aspect-name)})
         (nodes/update-node (:ticket @c/config) created-node-id))
    (delete-node created-node-id)))