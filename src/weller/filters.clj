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

(ns weller.filters
  (:require [clojure.core.async :as a]
            [cral.model.alfresco.cm :as cm])
  (:import (clojure.lang PersistentVector)))

(defn make-filtered-tap
  "Return a filtered tap connected to the `mult` channel.
  The returned tap is filtered by predicate `pred`."
  [mult pred]
  (a/tap mult (a/chan 1 (filter pred))))

(defn event?
  "Return true if message type is `event`.\\
  Example:
  ```clojure
  (event? events/node-updated)
  ```
  will return true when the message :type key is org.alfresco.event.node.Updated. Supported message types are defined in [[weller.events]]"
  [event]
  (partial #(= %1 (:type %2)) event))

(defn aspect-added?
  "Return true when `aspect` has been added to the node.\\
  Example:
  ```clojure
  (aspect-added? cm/asp-versionable)
  ```"
  [aspect]
  (partial #(and
              (.contains ^PersistentVector (get-in % [:data :resource :aspect-names]) aspect)
              (not (.contains ^PersistentVector (get-in % [:data :resource-before :aspect-names]) aspect)))))

(defn aspect-removed?
  "Return true when `aspect` has been removed from the node.\\
  Example:
  ```clojure
  (aspect-removed? cm/asp-versionable)
  ```"
  [aspect]
  (partial #(and
              (not (.contains ^PersistentVector (get-in % [:data :resource :aspect-names]) aspect))
              (.contains ^PersistentVector (get-in % [:data :resource-before :aspect-names]) aspect))))

(defn assoc-type?
  "Return true when an event correspond to a specific association type."
  [assoc-type]
  (partial #(= (get-in % [:data :resource :assoc-type]) (name assoc-type))))

(defn content-added?
  ;; TODO
  "Return true when content is added to an existing `cm:content`node."
  []
  (partial true?))

(defn content-changed?
  ;; TODO
  "Return true when the content of an existing `cm:content`node is updated."
  []
  )

(defn is-file?
  "Return true when the node is a file."
  []
  (partial #(= (get-in % [:data :resource :is-file]) true)))

(defn is-folder?
  "Return true when the node is a folder."
  []
  (partial #(= (get-in % [:data :resource :is-folder]) true)))

(defn mime-type?
  "Return true when a `cm:content` node has the given MIME type."
  [mime-type]
  (partial #(let [resource (get-in % [:data :resource])]
              (and
                (= (:node-type resource) (name cm/type-content))
                (= (get-in resource [:content :mime-type]) mime-type)))))

(defn node-aspect?
  "Return true when a node has the given `aspect`"
  [aspect]
  (partial #(let [resource (get-in % [:data :resource])]
              (and
                (= ((keyword "@type") resource) "NodeResource")
                (and (not (nil? (:aspect-names resource))) (.contains ^PersistentVector (:aspect-names resource) (name aspect)))))))