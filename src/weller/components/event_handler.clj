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

(ns weller.components.event-handler
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.telemere :as t]))

(defrecord EventHandler
  [chan f status]
  component/Lifecycle

  (start [this]
    (t/log! :info "starting EventHandler")
    (let [status (atom {})]
      (swap! status assoc :running true)
      (a/go-loop [message (a/<! chan)]
        (f (get-in message [:data :resource]))
        (when (:running @status) (recur (a/<! chan))))
      (assoc this :status status)))

  (stop [this]
    (t/log! :info "stopping EventHandler")
    (swap! status assoc :running false)
    this))

(defn make-handler [chan f]
  (map->EventHandler {:chan chan :f f}))