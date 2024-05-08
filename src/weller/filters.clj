(ns weller.filters
  (:require [clojure.core.async :as a]
            [weller.config :as c])
  (:import (clojure.lang PersistentVector)))

(defn make-filter
  "Returns a filtered tap from a predicate."
  [pred]
  (a/tap (:mult @c/state) (a/chan 1 (filter pred))))

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

(defn aspect-added?
  "Return true when `aspect` has been added to the node.\\
  Example:
  ```clojure
  (aspect-added? cm/asp-versionable)
  ```"
  [aspect]
  (partial #(and (.contains ^PersistentVector (get-in % [:data :resource :aspect-names]) aspect)
                 (not (.contains ^PersistentVector (get-in % [:data :resource-before :aspect-names]) aspect)))))