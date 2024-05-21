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

(ns weller.pipe-test
  (:require [clojure.test :refer :all]
            [cral.api.core.nodes :as nodes]
            [weller.components.component :as component]
            [weller.config :as c]
            [weller.events :as events]
            [weller.fixtures :as fixtures]
            [weller.pipe :as pipe]
            [weller.predicates :as pred]
            [weller.test-utils :as tu])
  (:import (java.util UUID)))

(use-fixtures :once fixtures/ticket)

(defn- get-node-name-with-cral
  [resource]
  (get-in (nodes/get-node (:ticket @c/config) (:id resource)) [:body :entry :name]))

(deftest make-pipe-test
  (let [result (promise)
        pipe (-> (pipe/make-pipe)
                 (pipe/add-filtered-tap (pred/event? events/node-created) #(deliver result (get-node-name-with-cral %)))
                 (component/start))
        node-name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node node-name)
    (is (= @result node-name))
    (component/stop pipe)))

(deftest simple-make-pipe-test
  (let [result (promise)
        ;; note that pipe is started automatically with this constructor
        pipe (pipe/make-pipe (pred/event? events/node-created) #(deliver result (get-node-name-with-cral %)))
        node-name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node node-name)
    (is (= @result node-name))
    (component/stop pipe)))

(deftest simple-make-pipe-double-start-double-stop-test
  (let [pipe (-> (pipe/make-pipe (pred/event? events/node-created) nil)
                 (component/start)
                 (component/stop))]
    (component/stop pipe)))

(deftest remove-then-add-tap-test
  (let [result (promise)
        ;; note that pipe is started automatically with this constructor
        pipe (-> (pipe/make-pipe (pred/event? events/node-deleted) (fn [_] (deliver result nil)))
                 (pipe/remove-taps)
                 (pipe/add-filtered-tap (pred/event? events/node-created) #(deliver result %))
                 (component/start))]
    (tu/create-then-update-then-delete-node)
    (is (not (nil? @result)))
    (component/stop pipe)))