(ns weller.predicates-extra
  (:require [cral.model.core :as model]
            [cral.api.core.nodes :as nodes]
            [weller.config :as c]))

(defn in-path?
  [path]
  (partial
    #(= (get-in
          (->> (model/map->GetNodeQueryParams {:include "path"})
               (nodes/get-node (c/ticket) (get-in % [:data :resource :id])))
          [:body :entry :path :name])
        path)))