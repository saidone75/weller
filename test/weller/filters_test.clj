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

(ns weller.filters-test
  (:require [clojure.test :refer :all]
            [cral.model.alfresco.cm :as cm]
            [weller.components.component :as component]
            [weller.event-handler :as handler]
            [weller.events :as events]
            [weller.filters :as filters]
            [weller.fixtures :as fixtures]
            [weller.test-utils :as tu])
  (:import (clojure.lang PersistentVector)))

(use-fixtures :once fixtures/ticket)

(deftest aspect-added-test
  (let [resource (promise)
        handler (handler/make-handler (every-pred (filters/event? events/node-updated) (filters/aspect-added? cm/asp-versionable)) #(deliver resource %))]
    (tu/add-aspect cm/asp-versionable)
    (is (.contains ^PersistentVector (:aspect-names @resource) (name cm/asp-versionable)))
    (component/stop handler)))