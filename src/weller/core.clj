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

(set! *warn-on-reflection* true)

(defn- at-exit
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

  (at-exit shutdown))