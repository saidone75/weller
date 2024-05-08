(ns weller.components.handlers.message-handler
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.telemere :as t]))

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