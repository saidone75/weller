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

(ns weller.handler
  (:require [clojure.core.async :as a]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.components.activemq :as activemq]
            [weller.components.component :as component]
            [weller.components.event-handler :as eh]
            [weller.config :as c]
            [weller.filters :as filters]))

(defrecord Handler
  [listener taps chan mult]
  component/Component

  (start [this]
    (assoc this
      :listener (component/start (:listener this))
      :taps (doall (map component/start (:taps this)))))

  (stop [this]
    (assoc this
      :taps (doall (map component/stop (:taps this)))
      :listener (component/stop (:listener this)))))

(defn add-tap [this pred f]
  (assoc this :taps (conj (:taps this) (eh/make-handler (filters/make-filter (:mult this) pred) f))))

(defn make-handler
  []
  ;; load config
  (try
    (reset! c/config (immu/load "resources/config.edn"))
    (catch Exception e (t/log! :error (.getMessage e))))
  (t/log! :debug @c/config)

  (let [chan (a/chan)]
    (map->Handler {:listener (activemq/make-listener (:activemq @c/config) chan)
                   :taps     []
                   :chan     chan
                   :mult     (a/mult chan)})))