(ns weller.core
  (:require [clojure.core.async :as a]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.model.alfresco.cm :as cm]
            [cral.utils.utils :as cu]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.events :as events]
            [weller.filters :as filters])
  (:import (jakarta.jms Session TextMessage)
           (org.apache.activemq ActiveMQConnection ActiveMQConnectionFactory))
  (:gen-class))

(set! *warn-on-reflection* true)

(def state (atom {}))

(defrecord ActiveMqListener
  [config ^ActiveMQConnection connection chan]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting ActiveMqListener")
    (if (and connection (.isStarted connection))
      this
      (let [connection-factory (new ActiveMQConnectionFactory)
            _ (. connection-factory (setBrokerURL (format "failover:(%s://%s:%d)" (:scheme config) (:host config) (:port config))))
            ^ActiveMQConnection connection (.createConnection connection-factory)
            session (.createSession connection false, Session/AUTO_ACKNOWLEDGE)
            topic (.createTopic session (:topic config))
            consumer (.createConsumer session topic)]
        (.start connection)
        (a/go-loop [^TextMessage message nil]
          (when-not (nil? message)
            (a/>! chan (cu/kebab-keywordize-keys (json/read-str (.getText message)))))
          (when (.isStarted connection) (recur (.receive consumer))))
        (assoc this :connection connection))))

    (stop [this]
          (t/log! :info "stopping ActiveMqListener")
          (if-not connection
            this
            (do
              (try
                (.close connection)
                (catch Throwable _
                  (t/log! :warn "Error while stopping component")))
              (assoc this :connection nil)))))

(defn make-activemq-listener [config chan]
  (map->ActiveMqListener {:config config :chan chan :status (atom {})}))

(defrecord MessageHandler
  [chan f status]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting MessageHandler")
    (let [status (atom {})]
      (swap! status assoc :running true)
      (a/go-loop [message (a/<!! chan)]
        (f message)
        (when (:running @status) (recur (a/<! chan))))
      (assoc this :status status)))

  (stop [this]
    (t/log! :info "stopping MessageHandler")
    (swap! status assoc :running false)
    this))

(defn make-handler [chan f]
  (map->MessageHandler {:chan chan :f f}))

(defrecord Application
  [config activemq-listener message-handler message-handler2]
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

(defn system [config]
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

  (let [component (component/start (system @config))]

    (loop []
      (Thread/sleep 1000)
      (recur))

    (Thread/sleep 30000)
    (component/stop component)
    (Thread/sleep 1000)))