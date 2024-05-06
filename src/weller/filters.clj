(ns weller.filters
  (:import (clojure.lang PersistentVector)))

(set! *warn-on-reflection* true)

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