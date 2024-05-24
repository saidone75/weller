(ns weller.predicates-extra
  (:require [cral.model.core :as model]
            [cral.api.core.nodes :as nodes]
            [weller.config :as c])
  (:import (java.util.regex Pattern)))

(defn in-path?
  "Checks the path of a node. If `p` is a string checks the exact path, if `p` is a java.util.regex.Pattern checks the
  match against the pattern."
  [p]
  (partial
    #(let [path (get-in
                  (->> (model/map->GetNodeQueryParams {:include "path"})
                       (nodes/get-node (c/ticket) (get-in % [:data :resource :id])))
                  [:body :entry :path :name])]
       (if (= (type p) Pattern)
         (re-matches p path)
         (= p path)))))