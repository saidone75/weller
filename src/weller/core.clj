(ns weller.core
  (:require [clojure.core.async :as a]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.utils.utils :as cu]
            [cral.model.alfresco.cm :as cm]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.events :as events]
            [weller.filters :as filters])
  (:import (jakarta.jms Connection Session TextMessage)
           (org.apache.activemq ActiveMQConnectionFactory))
  (:gen-class))

(set! *warn-on-reflection* true)

(def state (atom {}))

(defrecord ActiveMqListener
  [config ^Connection connection chan status]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting ActiveMqListener")
    (if connection
      this
      (try
        (let [connection-factory (new ActiveMQConnectionFactory)
              _ (. connection-factory (setBrokerURL (format "%s://%s:%d" (:scheme config) (:host config) (:port config))))
              ^Connection connection (.createConnection connection-factory)
              session (.createSession connection false, Session/AUTO_ACKNOWLEDGE)
              topic (.createTopic session (:topic config))
              consumer (.createConsumer session topic)
              status (atom {:running false})]
          (.start connection)
          (swap! status assoc :running true)
          (a/go-loop [^TextMessage message nil]
            (when-not (nil? message)
              (a/>! chan (cu/kebab-keywordize-keys (json/read-str (.getText message)))))
            (when (:running @status) (recur (.receive consumer))))
          (assoc this :status status :connection connection))
        (catch Exception e (do (t/log! :error (.getMessage e))
                               (assoc this :status (atom {:running false}) :connection nil))))))

  (stop [this]
    (t/log! :info "stopping ActiveMqListener")
    (swap! status assoc :running false)
    (if-not connection
      this
      (do
        (try
          (.close connection)
          (catch Throwable t
            (t/log! :warn "Error while stopping component")))
        (assoc this :connection nil)))))

(defn make-activemq-listener [config channel]
  (component/using
    (map->ActiveMqListener {:config config :channel channel})
    []))

(defrecord MessageHandler
  [channel fn status]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting MessageHandler")
    (let [status (atom {})]
      (swap! status assoc :running true)
      (a/go-loop [message (a/<!! channel)]
        (fn message)
        (when (:running @status) (recur (a/<! channel))))
      (assoc this :status status)))

  (stop [this]
    (t/log! :info "stopping MessageHandler")
    (swap! status assoc :running false)
    this))

(defn make-handler [chan f]
  (map->MessageHandler {:channel chan :fn f}))

(defrecord Application
  [config]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting Application")
    this)

  (stop [this]
    (t/log! :info "stopping Application")
    this))

(defn make-filter
  "Returns a filtered tap from a predicate."
  [pred]
  (a/tap (:mult @state) (a/chan 1 (filter pred))))

(defn make-application [config]
  (map->Application config))

(defn application [config]
  (component/system-map
    :activemq-listener (make-activemq-listener (:activemq config) (:chan @state))
    :message-handler (make-handler (make-filter (filters/event? events/node-created)) #(t/log! %))
    :message-handler2 (make-handler (make-filter (every-pred (filters/event? events/node-updated) (filters/is-file?) (filters/aspect-added? cm/asp-versionable))) #(t/log! %))
    :app (component/using
           (make-application config)
           [:activemq-listener :message-handler :message-handler2])))

(defn- exit
  [status msg]
  (if-not (nil? msg) (t/log! :info msg))
  (System/exit status))

(defn -main
  [& args]

  (swap! state assoc :chan (a/chan))
  (swap! state assoc :mult (a/mult (:chan @state)))

  ;; load configuration
  (def config (atom {}))
  (try
    (reset! config (immu/load "resources/config.edn"))
    (catch Exception e (exit 1 (.getMessage e))))
  (t/log! :debug @config)

  (let [component (component/start (application @config))]

    (loop []

      (t/log! @(:status (:activemq-listener component)))
      (Thread/sleep 1000)

      (recur))
    (Thread/sleep 30000)
    (component/stop component)
    (Thread/sleep 1000)))