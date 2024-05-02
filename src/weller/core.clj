(ns weller.core
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [cral.utils.utils :as cu])
  (:import (jakarta.jms Session)
           (org.apache.activemq ActiveMQConnectionFactory))
  (:gen-class))

(defrecord ActiveMqListener
  [config connection consumer]
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
            consumer (.createConsumer session topic)]
        (.start connection)
        (assoc this :connection connection :consumer consumer))))

  (stop [this]
    (println "stopping ActiveMqListener")
    (.close connection)
    (assoc this :connection nil :consumer nil)))

(defn make-activemq-listener []
  (component/using
    (map->ActiveMqListener {})
    []))

(defn get-message [listener]
  (let [message (.receive (:consumer listener))]
    (println message)
    (cu/kebab-keywordize-keys (json/read-str (.getText message)))))

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
    (component/stop component)))