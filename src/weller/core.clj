(ns weller.core
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [cral.model.alfresco.cm :as cm]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.components.handlers.message-handler :as handler]
            [weller.components.listeners.activemq :as activemq]
            [weller.config :as c]
            [weller.events :as events]
            [weller.filters :as filters])
  (:gen-class))

(set! *warn-on-reflection* true)

(defrecord Application
  [config activemq-listener message-handler message-handler2]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting Application")
    this)

  (stop [this]
    (t/log! :info "stopping Application")
    this))

(defn make-application [config]
  (map->Application config))

(defn system [config]
  (component/system-map
    :activemq-listener (activemq/make-listener (:activemq config) (:chan @c/state))
    :message-handler (handler/make-handler (filters/make-filter (filters/event? events/node-created)) #(t/log! %))
    :message-handler2 (handler/make-handler (filters/make-filter (every-pred (filters/event? events/node-updated) (filters/is-file?) (filters/aspect-added? cm/asp-versionable))) #(t/log! %))
    :app (component/using
           (make-application config)
           [:activemq-listener :message-handler :message-handler2])))

(defn- exit
  [status msg]
  (if-not (nil? msg) (t/log! :info msg))
  (System/exit status))

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

  (let [component (component/start (system @config))]

    (loop []
      (Thread/sleep 1000)
      (recur))))