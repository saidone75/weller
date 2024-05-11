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

(ns weller.filters
  (:require [clojure.core.async :as a])
  (:import (clojure.lang PersistentVector)))

(defn make-filter
  "Do not really make a filter, but returns a filtered (by predicate `pred`) tap connected to the `mult`."
  [mult pred]
  (a/tap mult (a/chan 1 (filter pred))))

(defn aspect-added?
  "Return true when `aspect` has been added to the node.\\
  Example:
  ```clojure
  (aspect-added? cm/asp-versionable)
  ```"
  [aspect]
  (partial #(and (.contains ^PersistentVector (get-in % [:data :resource :aspect-names]) aspect)
                 (not (.contains ^PersistentVector (get-in % [:data :resource-before :aspect-names]) aspect)))))

(defn aspect-removed?
  "Return true when `aspect` has been removed to the node.\\
  Example:
  ```clojure
  (aspect-removed? cm/asp-versionable)
  ```"
  [aspect]
  (partial #(and (not (.contains ^PersistentVector (get-in % [:data :resource :aspect-names]) aspect))
                 (.contains ^PersistentVector (get-in % [:data :resource-before :aspect-names]) aspect))))

(defn event?
  "Return true if message type is `event`.\\
  Example:
  ```clojure
  (event? events/node-updated)
  ```
  will return true when the message :type key is org.alfresco.event.node.Updated."
  [event]
  (partial #(= %1 (:type %2)) event))

(defn is-file?
  "Return true when the node is a file."
  []
  (partial #(= (get-in % [:data :resource :is-file]) true)))

