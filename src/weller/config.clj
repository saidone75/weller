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

(ns weller.config
  (:require [immuconf.config :as immu]
            [taoensso.telemere :as t]))

(def config (atom {:alfresco {:scheme         "http"
                              :host           "localhost"
                              :port           8080
                              :core-path      "alfresco/api/-default-/public/alfresco/versions/1"
                              :search-path    "alfresco/api/-default-/public/search/versions/1"
                              :auth-path      "alfresco/api/-default-/public/authentication/versions/1"
                              :discovery-path "alfresco/api/discovery"
                              :user           "admin"
                              :password       "admin"}
                   :activemq {:scheme "tcp"
                              :host   "localhost"
                              :port   61616
                              :topic  "alfresco.repo.event2"}}))

(defn configure
  []
  (try
    (let [cfg (immu/load "resources/config.edn")]
      (swap! config assoc :alfresco (merge (:alfresco @config) (:alfresco cfg)))
      (swap! config assoc :activemq (merge (:activemq @config) (:activemq cfg))))
    (catch Exception e (t/log! :error (.getMessage e))))
  (t/log! :debug @config))