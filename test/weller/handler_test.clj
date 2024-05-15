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

(ns weller.handler-test
  (:require [clojure.test :refer :all]
            [cral.api.core.nodes :as nodes]
            [taoensso.telemere :as t]
            [weller.components.component :as component]
            [weller.config :as c]
            [weller.event-handler :as handler]
            [weller.events :as events]
            [weller.filters :as filters]
            [weller.fixtures :as fixtures]
            [weller.test-utils :as tu]))

(use-fixtures :once fixtures/ticket)

(def node-name "test node name")

(defn- get-node-name
  [resource]
  (get-in (nodes/get-node (:ticket @c/config) (:id resource)) [:body :entry :name]))

(deftest make-handler-test
  (let [result (promise)
        handler (handler/make-handler)
        handler (handler/add-filtered-tap handler (filters/event? events/node-created) #(deliver result (get-node-name %)))]
    (component/start handler)
    (tu/create-then-update-then-delete-node node-name)
    (t/log! @result)
    (component/stop handler)))