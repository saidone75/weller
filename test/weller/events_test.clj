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

(ns weller.events-test
  (:require [clojure.test :refer :all]
            [weller.components.component :as component]
            [weller.core :refer :all]
            [weller.event-handler :as handler]
            [weller.events :as events]
            [weller.filters :as filters]
            [weller.fixtures :as fixtures]
            [weller.test-utils :as tu])
  (:import (java.util UUID)))

(use-fixtures :once fixtures/ticket)

(deftest node-created-test
  (let [resource (promise)
        handler (handler/make-handler (filters/event? events/node-created) #(deliver resource %))
        name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node name)
    (is (= (:name @resource) name))
    (component/stop handler)))

(deftest node-updated-test
  (let [resource (promise)
        handler (handler/make-handler (every-pred (filters/event? events/node-updated) (filters/is-file?)) #(deliver resource %))
        name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node name)
    (is (= (:name @resource) name))
    (component/stop handler)))

(deftest node-deleted-test
  (let [resource (promise)
        handler (handler/make-handler (filters/event? events/node-deleted) #(deliver resource %))
        name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node name)
    (is (= (:name @resource) name))
    (component/stop handler)))

(deftest child-assoc-created-test
  (let [resource (promise)
        handler (handler/make-handler (filters/event? events/child-assoc-created) #(deliver resource %))]
    (tu/create-then-delete-child-assoc)
    (is (= ((keyword "@type") @resource) "ChildAssociationResource"))
    (component/stop handler)))

(deftest child-assoc-deleted-test
  (let [resource (promise)
        handler (handler/make-handler (filters/event? events/child-assoc-deleted) #(deliver resource %))]
    (tu/create-then-delete-child-assoc)
    (is (= ((keyword "@type") @resource) "ChildAssociationResource"))
    (component/stop handler)))

(deftest peer-assoc-created-test
  (let [resource (promise)
        handler (handler/make-handler (filters/event? events/peer-assoc-created) #(deliver resource %))]
    (tu/create-then-delete-peer-assoc)
    (is (= ((keyword "@type") @resource) "PeerAssociationResource"))
    (component/stop handler)))

(deftest peer-assoc-deleted-test
  (let [resource (promise)
        handler (handler/make-handler (filters/event? events/peer-assoc-deleted) #(deliver resource %))]
    (tu/create-then-delete-peer-assoc)
    (is (= ((keyword "@type") @resource) "PeerAssociationResource"))
    (component/stop handler)))

;; enterprise only
(deftest permission-updated-test)