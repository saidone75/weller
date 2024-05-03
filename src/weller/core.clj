(ns weller.core
  (:require [clojure.core.async :as a]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.utils.utils :as cu]
            [immuconf.config :as immu]
            [taoensso.telemere :as t])
  (:import (jakarta.jms Session)
           (org.apache.activemq ActiveMQConnectionFactory))
  (:gen-class))

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
        (a/go-loop [message (if (:running @status) (.receive consumer) nil)]
          (when-not (nil? message)
            (a/>! channel (cu/kebab-keywordize-keys (json/read-str (.getText message)))))
          (when (:running @status) (recur (.receive consumer))))
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
  [config channel status]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting MessageHandler")
    (let [status (atom {})]
      (swap! status assoc :running true)
      (a/go-loop [message (a/<!! channel)]
        (t/log! message)
        (when (:running @status) (recur (a/<! channel))))
      (assoc this :status status)))

  (stop [this]
    (t/log! :info "stopping MessageHandler")
    (swap! status assoc :running false)
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
        handler1 (a/chan 1 (filter #(= (:type %) "org.alfresco.event.node.Updated")))
        handler2 (a/chan 1 (filter #(= (:type %) "org.alfresco.event.node.Created")))]
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