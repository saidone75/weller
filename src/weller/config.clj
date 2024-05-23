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

(ns weller.config
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [cral.api.auth :as auth]
            [immuconf.config :as immu]
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

(def ^:private cfg-files
  ["resources/weller.edn"
   "~/.weller/weller.edn"
   "./weller.edn"])

(defn deep-merge [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))

(defn- expand-home [path]
  (if (str/starts-with? path "~/")
    (str/replace path #"^~" (System/getProperty "user.home"))
    path))

(defn- extract-var [v]
  (let [v (rest (re-matches #"^\$\{([^:]+):(.*)\}$" (str v)))]
    (if-not (empty? v)
      v
      nil)))

(defn- resolve-placeholder [v]
  (if-let [var (extract-var v)]
    (if-let [val (System/getenv (first var))]
      val
      (last var))
    v))

(defn- parse-values
  [m f]
  (let [f (fn [[k v]] [k (if (map? v) v (f v))])]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn- load-cfg-file [file-name]
  (try
    (let [cfg (immu/load file-name)]
      (t/log! :info (format "loading config file %s" file-name))
      (swap! config deep-merge cfg)
      (reset! config (parse-values @config resolve-placeholder)))
    (catch Exception e (t/log! :warn (.getMessage e)))))

(defn configure
  []
  (run!
    load-cfg-file
    (map expand-home cfg-files))
  ;; configure CRAL
  (cral.config/configure (:alfresco @config))
  ;; authenticate on Alfresco and store ticket
  (swap! config assoc :alfresco
         (assoc (:alfresco @config) :ticket (get-in (auth/create-ticket (get-in @config [:alfresco :user]) (get-in @config [:alfresco :password])) [:body :entry])))
  (t/log! :trace @config))