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

(ns weller.components.message-handler
  (:require [clojure.core.async :as a]
            [taoensso.telemere :as t]
            [weller.components.component :as component]))

(def name "MessageHandler")

(defrecord MessageHandler
  [chan f running]
  component/Component

  (start [this]
    (t/log! :info (format "starting %s" name))
    (let [this (assoc this :running true)]
      (a/go-loop [message (a/<! chan)]
        (f (get-in message [:data :resource]))
        (when (:running this) (recur (a/<! chan))))
      this))

  (stop [this]
    (t/log! :info (format "stopping %s" name))
    (assoc this :running false)))

(defn make-handler [chan f]
  (map->MessageHandler {:chan chan :f f}))