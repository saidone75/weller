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

(ns weller.core
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [cral.model.alfresco.cm :as cm]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.components.application :as application]
            [weller.components.handlers.message-handler :as handler]
            [weller.components.listeners.activemq :as activemq]
            [weller.config :as c]
            [weller.events :as events]
            [weller.filters :as filters])
  (:gen-class))

(defn- on-exit
  [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable f)))

(defn- exit
  [status msg]
  (if-not (nil? msg) (t/log! :info msg))
  (System/exit status))

(defn- shutdown
  []
  ;; stop system
  (component/stop (:system @c/state))
  (shutdown-agents))

(defn system [config]
  (component/system-map
    :activemq-listener (activemq/make-listener (:activemq config) (:chan @c/state))
    :message-handler (handler/make-handler (filters/make-filter (filters/event? events/node-created)) #(t/log! %))
    :message-handler2 (handler/make-handler (filters/make-filter (every-pred (filters/event? events/node-updated) (filters/is-file?) (filters/aspect-added? cm/asp-versionable))) #(t/log! %))
    :app (component/using
           (application/make-application)
           [:activemq-listener :message-handler :message-handler2])))

(defn -main
  [& args]

  ;; set up channels
  (swap! c/state assoc :chan (a/chan))
  (swap! c/state assoc :mult (a/mult (:chan @c/state)))

  ;; load configuration
  (def config (atom {}))
  (try
    (reset! config (immu/load "resources/config.edn"))
    (catch Exception e (exit 1 (.getMessage e))))
  (t/log! :debug @config)

  ;; start system
  (swap! c/state assoc :system (component/start (system @config)))

  (on-exit shutdown))