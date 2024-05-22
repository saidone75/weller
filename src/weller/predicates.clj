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

(ns weller.predicates
  (:import (clojure.lang PersistentVector)))

(defn- is-node-resource?
  [resource]
  (= ((keyword "@type") resource) "NodeResource"))

(defn event?
  "Checks if message type is `event`."
  [event]
  (partial #(= (:type %) event)))

(defn aspect-added?
  "Checks if an event corresponds to a repository node that has had specified aspect added."
  [aspect]
  (partial #(let [aspect (name aspect)
                  aspects (get-in % [:data :resource :aspect-names])
                  aspects-before (get-in % [:data :resource-before :aspect-names])]
              (if-not (or (nil? aspects) (nil? aspects-before))
                (and
                  (.contains ^PersistentVector aspects aspect)
                  (not (.contains ^PersistentVector aspects-before aspect)))
                false))))

(defn aspect-removed?
  "Checks if an event corresponds to a repository node that has had specified aspect removed."
  [aspect]
  (partial #(let [aspect (name aspect)
                  aspects (get-in % [:data :resource :aspect-names])
                  aspects-before (get-in % [:data :resource-before :aspect-names])]
              (if-not (or (nil? aspects) (nil? aspects-before))
                (and
                  (not (.contains ^PersistentVector aspects aspect))
                  (.contains ^PersistentVector aspects-before aspect))
                false))))

(defn assoc-type?
  "Checks if an event corresponds to a specific association type. This doesn’t distinguish if the event is representing a peer-peer or parent-child association."
  [assoc-type]
  (partial #(= (get-in % [:data :resource :assoc-type]) (name assoc-type))))

(defn content-added?
  "Checks if an event represents the addition of content (i.e. a file) to an existing *cm:content* node in the repository."
  []
  (partial #(let [
                  content (get-in % [:data :resource :content])
                  content-before (get-in % [:data :resource-before :content])]
              (if-not (or (nil? content) (nil? content-before))
                (and
                  (= (:size-in-bytes content-before) 0)
                  (> (:size-in-bytes content) 0))
                false))))

(defn content-changed?
  "Checks if an event represents a content update (i.e. file updated) of a *cm:content* node in the repository."
  []
  (partial #(let [content (get-in % [:data :resource :content])
                  content-before (get-in % [:data :resource-before :content])]
              (if-not (or (nil? content) (nil? content-before))
                (and
                  (not (= (:size-in-bytes content-before) 0))
                  (or
                    (not (= (:size-in-bytes content) (:size-in-bytes content-before)))
                    (not (= (:mime-type content) (:mime-type content-before)))
                    (not (= (:encoding content) (:encoding content-before)))))
                false))))

(defn is-file?
  "Checks if an event corresponds to a repository node of type *cm:content* or subtype (i.e. a file)."
  []
  (partial #(get-in % [:data :resource :is-file])))

(defn is-folder?
  "Checks if an event corresponds to a repository node of type *cm:folder* or subtype (i.e. a folder)."
  []
  (partial #(get-in % [:data :resource :is-folder])))

(defn mime-type?
  "Checks if an event represents a content node (i.e. *cm:content*) with a specific MIME type."
  [mime-type]
  (partial #(let [resource (get-in % [:data :resource])]
              (and
                (:is-file resource)
                (= (get-in resource [:content :mime-type]) mime-type)))))

(defn node-aspect?
  "Checks if an event represents a node with a specific `aspect`."
  [aspect]
  (partial #(let [resource (get-in % [:data :resource])]
              (and
                (is-node-resource? resource)
                (and (not (nil? (:aspect-names resource))) (.contains ^PersistentVector (:aspect-names resource) (name aspect)))))))

(defn node-moved?
  "Checks if an event represents a node being moved in the repository."
  []
  (partial #(let [resource-before (get-in % [:data :resource-before])]
              (and
                (is-node-resource? resource-before)
                (not (empty? (:primary-hierarchy resource-before)))))))

(defn node-type-changed?
  "Checks if an event represents the change of the type of a node in the repository."
  []
  (partial #(let [type (get-in % [:data :resource :node-type])
                  type-before (get-in % [:data :resource-before :node-type])]
              (and
                (not (nil? type-before))
                (not (= type type-before))))))

(defn node-type?
  "Checks if an event represents a node with a specific type."
  [type]
  (partial #(= (get-in % [:data :resource :node-type]) (name type))))

(defn property-added?
  "Checks if an event corresponds to the addition of a node property in the repository."
  [prop]
  (partial #(let [properties (get-in % [:data :resource :properties])
                  properties-before (get-in % [:data :resource-before :properties])]
              (if-not (or (nil? properties) (nil? properties-before))
                (and
                  (or (not (contains? properties-before prop))
                      (nil? (get prop properties-before)))
                  (contains? properties prop))
                false))))

(defn property-changed?
  "Checks if an event corresponds to the update of a node property in the repository."
  [prop]
  (partial #(let [properties (get-in % [:data :resource :properties])
                  properties-before (get-in % [:data :resource-before :properties])]
              (if-not (or (nil? properties) (nil? properties-before))
                (and
                  (not (nil? (get properties-before prop)))
                  (not (nil? (get properties prop)))
                  (not (= (get properties-before prop) (get properties prop))))
                false))))

(defn property-current-value?
  "Checks if an event represents a node with a specific property with a specific current value."
  [prop value]
  (partial #(= (get (get-in % [:data :resource :properties]) prop) value)))

(defn property-removed?
  "Checks if an event corresponds to the removal of a specific property to a node in the repository."
  [prop]
  (partial #(let [properties (get-in % [:data :resource :properties])
                  properties-before (get-in % [:data :resource-before :properties])]
              (if-not (or (nil? properties) (nil? properties-before))
                (and
                  (or (not (contains? properties prop))
                      (nil? (get properties prop)))
                  (contains? properties-before prop))
                false))))

(defn property-previous-value?
  "Checks if an event represents a node with a specific property with a specific previous value."
  [prop value]
  (partial #(let [properties-before (get-in % [:data :resource-before :properties])]
              (if-not (nil? properties-before)
                (= (get properties-before prop) value)
                false))))

(defn property-value?
  "Checks if an event represents a node with a specific property with a specific value."
  [prop value]
  (partial #(let [properties (get-in % [:data :resource :properties])]
              (if-not (nil? properties)
                (= (get properties prop) value)
                false))))