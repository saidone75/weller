;
; Weller - Here's your servant, Sir. Out o' the process.
; Copyright (C) 2023-2026 Saidone
; SPDX-License-Identifier: GPL-3.0-or-later
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;

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