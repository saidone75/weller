;  weller
;  Copyright (C) 2024 Saidone
;
;  This program is free software: you can redistribute it and/or modify
;  it under the terms of the GNU General Public License as published by
;  the Free Software Foundation, either version 3 of the License, or
;  (at your option) any later version.
;
;  This program is distributed in the hope that it will be useful,
;  but WITHOUT ANY WARRANTY; without even the implied warranty of
;  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;  GNU General Public License for more details.
;
;  You should have received a copy of the GNU General Public License
;  along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns weller.components.activemq
  (:require [clojure.core.async :as a]
            [clojure.data.json :as json]
            [cral.utils.utils :as cu]
            [taoensso.telemere :as t]
            [weller.components.component :as component])
  (:import (jakarta.jms Session TextMessage)
           (org.apache.activemq ActiveMQConnection ActiveMQConnectionFactory)))

(defrecord Listener
  [config connection chan]
  component/Component

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
            (println "message from amq " message)
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
  (map->Listener {:config config :chan chan}))