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

(ns weller.core-test
  (:require [clojure.test :refer :all]
            [weller.components.component :as component]
            [weller.core :refer :all]
            [weller.event-handler :as handler]
            [weller.events :as events]
            [weller.filters :as filters]
            [weller.test-utils :as tu])
  (:import (java.util UUID)))

(deftest node-created-test
  (def resource (promise))
  (def handler (handler/make-handler (filters/event? events/node-created) #(deliver resource %)))
  (let [name (.toString (UUID/randomUUID))]
    (tu/create-then-delete-node name)
    (is (= (:name @resource)) name))
  (component/stop handler))