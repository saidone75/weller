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

(ns weller.filters-test
  (:require [clojure.test :refer :all]
            [cral.model.alfresco.cm :as cm]
            [weller.components.component :as component]
            [weller.events :as events]
            [weller.pipe :as pipe]
            [weller.predicates :as pred]
            [weller.test-utils :as tu])
  (:import (clojure.lang PersistentVector)
           (java.util UUID)))

(deftest aspect-added-test
  (let [result (promise)
        pipe (pipe/make-pipe (every-pred (pred/event? events/node-updated) (pred/aspect-added? cm/asp-versionable)) #(deliver result %))]
    (tu/add-then-remove-aspect cm/asp-versionable)
    (is (.contains ^PersistentVector (:aspect-names @result) (name cm/asp-versionable)))
    (component/stop pipe)))

(deftest aspect-removed-test
  (let [result (promise)
        pipe (pipe/make-pipe (every-pred (pred/event? events/node-updated) (pred/aspect-removed? cm/asp-versionable)) #(deliver result %))]
    (tu/add-then-remove-aspect cm/asp-versionable)
    (is (not (.contains ^PersistentVector (:aspect-names @result) (name cm/asp-versionable))))
    (component/stop pipe)))

(deftest assoc-type-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/assoc-type? cm/assoc-contains) #(deliver result %))]
    (tu/create-then-delete-peer-assoc)
    (is (= (:assoc-type @result) (name cm/assoc-contains)))
    (component/stop pipe)))

(deftest content-added-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/content-added?) #(deliver result %))]
    (tu/create-then-update-then-delete-node)
    (is (> (get-in @result [:content :size-in-bytes]) 0))
    (component/stop pipe)))

(deftest content-changed-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/content-changed?) #(deliver result %))]
    (tu/create-then-update-then-delete-node)
    (not (nil? @result))
    (component/stop pipe)))

(deftest is-file-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/is-file?) #(deliver result %))]
    (tu/create-then-update-then-delete-node)
    (is (:is-file @result))
    (component/stop pipe)))

(deftest is-folder-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/is-folder?) #(deliver result %))]
    (tu/create-then-update-then-delete-node)
    (is (:is-folder @result))
    (component/stop pipe)))

(deftest mime-type-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/mime-type? "text/plain") #(deliver result %))]
    (tu/create-then-update-then-delete-node)
    (is (= (get-in @result [:content :mime-type]) "text/plain"))
    (component/stop pipe)))

(deftest node-aspect-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/node-aspect? cm/asp-versionable) #(deliver result %))]
    (tu/add-then-remove-aspect cm/asp-versionable)
    (is (.contains ^PersistentVector (:aspect-names @result) (name cm/asp-versionable)))
    (component/stop pipe)))

(deftest node-moved-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/node-moved?) #(deliver result %))]
    (tu/create-then-move-node)
    (is (= ((keyword "@type") @result) "NodeResource"))
    (component/stop pipe)))

(deftest node-type-changed-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/node-type-changed?) #(deliver result %))]
    (tu/change-type cm/type-savedquery)
    (is (= (:node-type @result) (name cm/type-savedquery)))
    (component/stop pipe)))

(deftest node-type-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/node-type? cm/type-savedquery) #(deliver result %))]
    (tu/change-type cm/type-savedquery)
    (is (= (:node-type @result) (name cm/type-savedquery)))
    (component/stop pipe)))

(deftest property-added-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/property-added? cm/prop-publisher) #(deliver result %))]
    (tu/add-then-remove-property cm/prop-publisher)
    (is (contains? (:properties @result) cm/prop-publisher))
    (component/stop pipe)))

(deftest property-changed-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/property-changed? cm/prop-publisher) #(deliver result %))]
    (tu/change-property cm/prop-publisher)
    (is (contains? (:properties @result) cm/prop-publisher))
    (component/stop pipe)))

(deftest property-current-value-test
  (let [result (promise)
        value (.toString (UUID/randomUUID))
        pipe (pipe/make-pipe (pred/property-current-value? cm/prop-publisher value) #(deliver result %))]
    (tu/add-then-remove-property cm/prop-publisher value)
    (is (= (get-in @result [:properties cm/prop-publisher]) value))
    (component/stop pipe)))

(deftest property-removed-test
  (let [result (promise)
        pipe (pipe/make-pipe (pred/property-removed? cm/prop-publisher) #(deliver result %))]
    (tu/add-then-remove-property cm/prop-publisher)
    (is (nil? (get-in @result [:properties cm/prop-publisher])))
    (component/stop pipe)))

(deftest property-previous-value-test
  (let [result (promise)
        value (.toString (UUID/randomUUID))
        pipe (pipe/make-pipe (pred/property-previous-value? cm/prop-publisher value) #(deliver result %))]
    (tu/change-property cm/prop-publisher value)
    (is (not (nil? @result)))
    (component/stop pipe)))

(deftest property-value-test
  (let [result (promise)
        value (.toString (UUID/randomUUID))
        pipe (pipe/make-pipe (pred/property-value? cm/prop-publisher value) #(deliver result %))]
    (tu/change-property cm/prop-publisher value)
    (is (= (get-in @result [:properties cm/prop-publisher]) value))
    (component/stop pipe)))