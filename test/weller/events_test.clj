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

(ns weller.events-test
  (:require [clojure.test :refer :all]
            [weller.components.component :as component]
            [weller.core :refer :all]
            [weller.events :as events]
            [weller.pipe :as pipe]
            [weller.predicates :as pred]
            [weller.test-utils :as tu])
  (:import (java.util UUID)))

(deftest node-created-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/event? events/node-created) #(deliver result %))
        name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node name)
    (is (= (:name @result) name))
    (component/stop pipe)))

(deftest node-updated-test
  (let [result (promise)
        pipe (pipe/make-pipe (every-pred (pred/event? events/node-updated) (pred/is-file?)) #(deliver result %))
        name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node name)
    (is (= (:name @result) name))
    (component/stop pipe)))

(deftest node-deleted-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/event? events/node-deleted) #(deliver result %))
        name (.toString (UUID/randomUUID))]
    (tu/create-then-update-then-delete-node name)
    (is (= (:name @result) name))
    (component/stop pipe)))

(deftest child-assoc-created-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/event? events/child-assoc-created) #(deliver result %))]
    (tu/create-then-delete-child-assoc)
    (is (= ((keyword "@type") @result) "ChildAssociationResource"))
    (component/stop pipe)))

(deftest child-assoc-deleted-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/event? events/child-assoc-deleted) #(deliver result %))]
    (tu/create-then-delete-child-assoc)
    (is (= ((keyword "@type") @result) "ChildAssociationResource"))
    (component/stop pipe)))

(deftest peer-assoc-created-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/event? events/peer-assoc-created) #(deliver result %))]
    (tu/create-then-delete-peer-assoc)
    (is (= ((keyword "@type") @result) "PeerAssociationResource"))
    (component/stop pipe)))

(deftest peer-assoc-deleted-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/event? events/peer-assoc-deleted) #(deliver result %))]
    (tu/create-then-delete-peer-assoc)
    (is (= ((keyword "@type") @result) "PeerAssociationResource"))
    (component/stop pipe)))

;; enterprise only
(deftest permission-updated-test)