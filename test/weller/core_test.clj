(ns weller.core-test
  (:require [clojure.data.json :as json]
            [clojure.test :refer :all]
            [cral.utils.utils :as cu]
            [weller.core :refer :all])
  (:import (jakarta.jms Session)
           (org.apache.activemq ActiveMQConnectionFactory)))

(deftest receive-message-test
  (def connFactory (new ActiveMQConnectionFactory))
  (. connFactory (setBrokerURL "tcp://localhost:61616"))
  (def conn (.createConnection connFactory))
  (def sess (.createSession conn false, Session/AUTO_ACKNOWLEDGE))
  (def dest (.createTopic sess "alfresco.repo.event2"))
  (def cons (.createConsumer sess dest))
  (.start conn)
  (def msg (cu/kebab-keywordize-keys (json/read-str (.getText (.receive cons)))))
  (println msg)
  (println (:type msg))
  (.close conn))