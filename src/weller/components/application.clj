(ns weller.components.application
  (:require [com.stuartsierra.component :as component]
            [taoensso.telemere :as t]))

(defrecord Application
  [config]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting application")
    this)

  (stop [this]
    (t/log! :info "stopping application")
    this))

(defn make-application []
  (map->Application {}))