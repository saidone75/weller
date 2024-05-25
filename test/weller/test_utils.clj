;  Weller is like Alfresco out-of-process extensions but in Clojure
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
  (:require [clojure.java.io :as io]
            [clojure.math :as math]
            [clojure.test :refer :all]
            [cral.api.core.nodes :as nodes]
            [cral.model.alfresco.cm :as cm]
            [cral.model.core :as model]
            [weller.config :as c])
  (:import (java.io File)
           (java.util UUID)))

(defn- gen-str
  [length]
  (apply str (take length (repeatedly #(rand-nth (map char (range 65 90)))))))

(defn- get-guest-home
  []
  (get-in (nodes/get-node (c/ticket) "-root-" (model/map->GetNodeQueryParams {:relative-path "/Guest Home"})) [:body :entry :id]))

(defn- create-folder
  []
  (->> (model/map->CreateNodeBody {:name (.toString (UUID/randomUUID)) :node-type cm/type-folder})
       (nodes/create-node (c/ticket) (get-guest-home))
       (#(get-in % [:body :entry :id]))))

(defn- create-node
  ([]
   (create-node (.toString (UUID/randomUUID))))
  ([name]
   (create-node name (get-guest-home)))
  ([name parent-id]
   (->> (model/map->CreateNodeBody {:name name :node-type cm/type-content})
        (nodes/create-node (c/ticket) parent-id)
        (#(get-in % [:body :entry :id])))))

(defn- update-node
  [node-id]
  (->> (model/map->UpdateNodeBody {:properties {cm/prop-title (.toString (UUID/randomUUID))}})
       (nodes/update-node (c/ticket) node-id)
       (#(get-in % [:body :entry :id]))))

(defn- update-node-content
  [node-id]
  (let [file-to-be-uploaded (File/createTempFile "tmp." ".txt")]
    (spit file-to-be-uploaded (gen-str (rand-int (math/pow 2 16))))
    (nodes/update-node-content (c/ticket) node-id file-to-be-uploaded)
    (io/delete-file file-to-be-uploaded)
    node-id))

(defn- delete-node
  [node-id]
  (nodes/delete-node (c/ticket) node-id {:permanent true}))

(defn create-then-update-then-delete-node
  ([]
   (create-then-update-then-delete-node (.toString (UUID/randomUUID))))
  ([name]
   (->> (create-node name)
        (update-node)
        (update-node-content)
        (update-node-content)
        (delete-node))))

(defn create-then-move-node
  []
  (let [node-id (create-node)
        folder-id (create-folder)]
    (->> (model/map->MoveNodeBody {:target-parent-id folder-id})
         (nodes/move-node (c/ticket) node-id))
    (delete-node node-id)
    (delete-node folder-id)))

(defn change-type
  [type]
  (let [created-node-id (create-node)]
    (->> (model/map->UpdateNodeBody {:node-type type})
         (nodes/update-node (c/ticket) created-node-id))
    (delete-node created-node-id)))

(defn add-then-remove-property
  ([prop]
   (add-then-remove-property prop (.toString (UUID/randomUUID))))
  ([prop value]
   (let [created-node-id (create-node)]
     (->> (model/map->UpdateNodeBody {:properties {prop value}})
          (nodes/update-node (c/ticket) created-node-id))
     (->> (model/map->UpdateNodeBody {:properties {prop nil}})
          (nodes/update-node (c/ticket) created-node-id))
     (delete-node created-node-id))))

(defn change-property
  ([prop]
   (change-property prop (.toString (UUID/randomUUID))))
  ([prop initial-value]
   (let [created-node-id (create-node)]
     (->> (model/map->UpdateNodeBody {:properties {prop initial-value}})
          (nodes/update-node (c/ticket) created-node-id))
     (->> (model/map->UpdateNodeBody {:properties {prop (.toString (UUID/randomUUID))}})
          (nodes/update-node (c/ticket) created-node-id))
     (delete-node created-node-id))))

(defn create-then-delete-child-assoc
  []
  (let [parent-node-id (create-folder)
        child-node-id (create-node)]
    (->> [(model/map->CreateSecondaryChildBody {:child-id child-node-id :assoc-type cm/assoc-contains})]
         (nodes/create-secondary-child (c/ticket) parent-node-id))
    (nodes/delete-secondary-child (c/ticket) parent-node-id child-node-id)
    (delete-node child-node-id)
    (delete-node parent-node-id)))

(defn create-then-delete-peer-assoc
  []
  (let [created-node-id (create-node)]
    (->> (model/map->CreateNodeAssocsBody {:target-id created-node-id :assoc-type cm/assoc-contains})
         (nodes/create-node-assocs (c/ticket) (get-guest-home)))
    (nodes/delete-node-assocs (c/ticket) (get-guest-home) created-node-id)
    (delete-node created-node-id)))

(defn add-then-remove-aspect
  [aspect-name]
  (let [created-node-id (create-node)
        aspect-names (get-in (nodes/get-node (c/ticket) created-node-id) [:body :entry :aspect-names])]
    (->> (model/map->UpdateNodeBody {:aspect-names (conj aspect-names aspect-name)})
         (nodes/update-node (c/ticket) created-node-id))
    (->> (model/map->UpdateNodeBody {:aspect-names aspect-names})
         (nodes/update-node (c/ticket) created-node-id))
    (delete-node created-node-id)))