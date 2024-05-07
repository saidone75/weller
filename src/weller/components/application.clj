(ns weller.components.application
  (:require [taoensso.telemere :as t])
  (:import (com.stuartsierra.component Lifecycle)))

(defrecord Application
  [config]
  Lifecycle

  (start [this]
    (t/log! :info "starting application")
    this)

  (stop [this]
    (t/log! :info "stopping application")
    this))

(defn make-application []
  (map->Application {}))