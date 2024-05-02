(ns weller.core
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.utils.utils :as cu])
  (:import (jakarta.jms Session)
           (org.apache.activemq ActiveMQConnectionFactory))
  (:gen-class))

(defn read-message [consumer status]
  (println "starting read-message")
  (loop [message nil]
    (println @status)
    (when-not (nil? message)
      (println (cu/kebab-keywordize-keys (json/read-str (.getText message)))))
    (when (:running @status)
      (let [message (try (.receive consumer)
                         (catch Exception e (println (.getMessage e))))]
        (recur message))))
  (println "stopping read-message"))

(defrecord ActiveMqListener
  [config status connection consumer]
  component/Lifecycle

  (start [this]
    (println "starting ActiveMqListener")
    (if connection
      this
      (let [connection-factory (new ActiveMQConnectionFactory)
            _ (. connection-factory (setBrokerURL "tcp://localhost:61616"))
            connection (.createConnection connection-factory)
            session (.createSession connection false, Session/AUTO_ACKNOWLEDGE)
            topic (.createTopic session "alfresco.repo.event2")
            consumer (.createConsumer session topic)
            status (atom {})]
        (swap! status assoc :running true)
        (.start connection)
        (.start (Thread. #(read-message consumer status)))
        (assoc this :status status :connection connection :consumer consumer))))

  (stop [this]
    (println "stopping ActiveMqListener")
    (swap! status assoc :running false)
    (.close connection)
    (assoc this :connection nil :consumer nil)))

(defn make-activemq-listener []
  (component/using
    (map->ActiveMqListener {})
    []))

(defrecord Application [config activemq-listener]
  component/Lifecycle

  (start [this]
    (println "Starting Application")
    this)

  (stop [this]
    (println "Stopping Application")
    this))

(defn make-application [config]
  (map->Application config))

(defn application [config]
  (component/system-map
    :activemq-listener (make-activemq-listener)
    :app (component/using
           (make-application config)
           [:activemq-listener])))

(defn -main
  [& args]
  (let [component (component/start (application {}))]
    (Thread/sleep 30000)
    (component/stop component)
    (Thread/sleep 1000)))