;
; Weller - At your service. Out of process.
; Copyright (C) 2023-2026 Saidone
; SPDX-License-Identifier: GPL-3.0-or-later
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;

(ns weller.components.activemq-listener
  (:require [clojure.core.async :as a]
            [clojure.data.json :as json]
            [cral.utils.utils :as cu]
            [taoensso.telemere :as t]
            [weller.components.component :as component])
  (:import (jakarta.jms Session TextMessage)
           (org.apache.activemq ActiveMQConnection ActiveMQConnectionFactory)))

(def ^:const component-name "ActiveMQ listener")

(defn- parse-message [message]
  (try
    (when-not (instance? TextMessage message)
      (throw (ex-info "Expected a JMS TextMessage" {})))
    (let [event (json/read-str (.getText ^TextMessage message))]
      (when-not (map? event)
        (throw (ex-info "Expected a JSON object" {})))
      (cu/kebab-keywordize-keys event))
    (catch Exception e
      (t/log! {:level :error :error e}
              "Invalid JMS message; skipping message")
      nil)))

(defrecord ActiveMqListener
  [config connection chan]
  component/Component

  (start [this]
    (t/log! :info (format "starting %s" component-name))
    (if connection
      (do
        (t/log! :warn (format "%s is already running" component-name))
        this)
      (let [connection-factory (new ActiveMQConnectionFactory)
            _ (. connection-factory (setBrokerURL (format "failover:(%s://%s:%d)" (:scheme config) (:host config) (:port config))))
            ^ActiveMQConnection connection (.createConnection connection-factory)
            session (.createSession connection false, Session/AUTO_ACKNOWLEDGE)
            topic (.createTopic session (:topic config))
            consumer (.createConsumer session topic)]
        (.start connection)
        (a/go-loop [message nil]
          (when-not (nil? message)
            (when-let [message (parse-message message)]
              (t/trace! message)
              (a/>! chan message)))
          (when (.isStarted connection) (recur (.receive consumer))))
        (assoc this :connection connection))))

  (stop [this]
    (t/log! :info (format "stopping %s" component-name))
    (if-not connection
      (do
        (t/log! :warn (format "%s is not running" component-name))
        this)
      (do
        (try
          (.close connection)
          (catch Throwable _
            (t/log! :warn (format "error while stopping %s" component-name))))
        (assoc this :connection nil)))))

(defn make-listener
  "Creates an ActiveMQ listener with configuration map `config`. Received messages will be put in `chan`."
  [config chan]
  (map->ActiveMqListener {:config     config
                          :connection nil
                          :chan       chan}))
