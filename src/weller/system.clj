(ns weller.system
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [cral.model.alfresco.cm :as cm]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.components.activemq :as activemq]
            [weller.components.application :as application]
            [weller.components.event-handler :as handler]
            [weller.config :as c]
            [weller.events :as events]
            [weller.filters :as filters])
  (:import (java.io File)
           (java.util UUID)))


(defn- start-system []
  (component/system-map
    :activemq-listener (activemq/make-listener (:activemq @c/config) (:chan @c/state))
    ;;:event-handler1 (handler/make-handler (filters/make-filter (filters/event? events/node-created)) #(t/log! %))
    ;;:event-handler2 (handler/make-handler (filters/make-filter (every-pred (filters/event? events/node-updated) (filters/is-file?) (filters/aspect-added? cm/asp-versionable))) #(t/log! %))
    ;:app (component/using (application/make-application) [:activemq-listener :event-handler1 :event-handler2])
    ))

(defn start [system]
  (component/start system))

(defn stop [system]
  (component/stop system))


(defn add-handler [system handler]
  (swap! system assoc (keyword (.toString (UUID/randomUUID))) handler))

(defn make-system []
  ;; load config
  (try
    (reset! c/config (immu/load "resources/config.edn"))
    (catch Exception e (t/log! :error (.getMessage e))))
  (t/log! :debug @c/config)

  ;; set up channels
  (swap! c/state assoc :chan (a/chan))
  (swap! c/state assoc :mult (a/mult (:chan @c/state)))

  (swap! system assoc :activemq-listener (activemq/make-listener (:activemq @c/config) (:chan @c/state)))


  system

  )
