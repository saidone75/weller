(ns weller.core
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.utils.utils :as cu]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [clojure.core.async :as a])
  (:import (jakarta.jms Session)
           (org.apache.activemq ActiveMQConnectionFactory))
  (:gen-class))

(defn read-message [consumer channel status]
  (t/log! :debug "starting read-message")
  (loop [message nil]
    (t/log! :debug @status)
    (when-not (nil? message)
      (a/>!! channel (cu/kebab-keywordize-keys (json/read-str (.getText message))))
      ;(t/log! :debug (cu/kebab-keywordize-keys (json/read-str (.getText message))))
      )
    (when (:running @status)
      (let [message (try (.receive consumer)
                         (catch Exception e (println (.getMessage e))))]
        (recur message))))
  (t/log! :debug "stopping read-message"))

(defrecord ActiveMqListener
  [config connection channel status]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting ActiveMqListener")
    (if connection
      this
      (let [connection-factory (new ActiveMQConnectionFactory)
            _ (. connection-factory (setBrokerURL (format "%s://%s:%d" (:scheme config) (:host config) (:port config))))
            connection (.createConnection connection-factory)
            session (.createSession connection false, Session/AUTO_ACKNOWLEDGE)
            topic (.createTopic session (:topic config))
            consumer (.createConsumer session topic)
            status (atom {})]
        (swap! status assoc :running true)
        (.start connection)
        (.start (Thread. #(read-message consumer channel status)))
        (assoc this :status status :connection connection))))

  (stop [this]
    (t/log! :info "stopping ActiveMqListener")
    (swap! status assoc :running false)
    (.close connection)
    (assoc this :connection nil)))

(defn make-activemq-listener [config channel]
  (component/using
    (map->ActiveMqListener {:config config :channel channel})
    []))

(defrecord MessageHandler
  [config channel]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting MessageHandler")
    (t/log! (a/<!! channel))
    this)

  (stop [this]
    (t/log! :info "stopping MessageHandler")
    this))

(defn make-message-handler [config channel]
  (map->MessageHandler {:config config :channel channel}))

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

(defn application [config]
  (let [chan (a/chan)
        mult (a/mult chan)
        handler1 (a/chan)
        handler2 (a/chan)]
    (component/system-map
      :activemq-listener (make-activemq-listener (:activemq config) chan)
      :message-handler (make-message-handler (:alfresco config) (a/tap mult handler1))
      :message-handler2 (make-message-handler (:alfresco config) (a/tap mult handler2))
      :app (component/using
             (make-application config)
             [:activemq-listener :message-handler :message-handler2]))))

(defn- exit
  [status msg]
  (if-not (nil? msg) (t/log! :info msg))
  (System/exit status))

(defn -main
  [& args]

  ;; load configuration
  (def config (atom {}))
  (try
    (reset! config (immu/load "resources/config.edn"))
    (catch Exception e (exit 1 (.getMessage e))))
  (t/log! :debug @config)

  (let [component (component/start (application @config))]
    (Thread/sleep 30000)
    (component/stop component)
    (Thread/sleep 1000)))