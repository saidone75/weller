(ns weller.components.listeners.activemq
  (:require [clojure.core.async :as a]
            [clojure.data.json :as json]
            [cral.utils.utils :as cu]
            [taoensso.telemere :as t])
  (:import (com.stuartsierra.component Lifecycle)
           (jakarta.jms Session TextMessage)
           (org.apache.activemq ActiveMQConnection ActiveMQConnectionFactory)))

(defrecord Listener
  [config ^ActiveMQConnection connection chan]
  Lifecycle

  (start [this]
    (t/log! :info "starting ActiveMQ listener")
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
    (t/log! :info "stopping ActiveMQ listener")
    (if-not connection
      this
      (do
        (try
          (.close connection)
          (catch Throwable _
            (t/log! :warn "error while stopping component")))
        (assoc this :connection nil)))))

(defn make-listener [config chan]
  (map->Listener {:config config :chan chan :status (atom {})}))