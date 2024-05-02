(ns weller.core
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.utils.utils :as cu]
            [immuconf.config :as immu]
            [taoensso.telemere :as t])
  (:import (jakarta.jms Session)
           (org.apache.activemq ActiveMQConnectionFactory))
  (:gen-class))

(defn read-message [consumer status]
  (t/log! :debug "starting read-message")
  (loop [message nil]
    (t/log! :debug @status)
    (when-not (nil? message)
      (t/log! :debug (cu/kebab-keywordize-keys (json/read-str (.getText message)))))
    (when (:running @status)
      (let [message (try (.receive consumer)
                         (catch Exception e (println (.getMessage e))))]
        (recur message))))
  (t/log! :debug "stopping read-message"))

(defrecord ActiveMqListener
  [config status connection consumer]
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
        (.start (Thread. #(read-message consumer status)))
        (assoc this :status status :connection connection :consumer consumer))))

  (stop [this]
    (t/log! :info "stopping ActiveMqListener")
    (swap! status assoc :running false)
    (.close connection)
    (assoc this :connection nil :consumer nil)))

(defn make-activemq-listener [config]
  (component/using
    (map->ActiveMqListener {:config config})
    []))

(defrecord Application [config activemq-listener]
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
  (component/system-map
    :activemq-listener (make-activemq-listener (:activemq config))
    :app (component/using
           (make-application config)
           [:activemq-listener])))

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