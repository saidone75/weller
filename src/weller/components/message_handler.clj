;
; Weller - Here's your servant, Sir. Out o' the process.
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

(ns weller.components.message-handler
  (:require [clojure.core.async :as a]
            [taoensso.telemere :as t]
            [weller.components.component :as component]))

(def ^:const component-name "MessageHandler")

(defrecord MessageHandler
  [chan f running]
  component/Component

  (start [this]
    (if running
      (do
        (t/log! :warn (format "%s is already running" component-name))
        this)
      (do
        (t/log! :info (format "starting %s" component-name))
        (let [stop-chan (a/chan)
              worker (a/thread
                       (loop []
                         (let [[message port] (a/alts!! [stop-chan chan] :priority true)]
                           (when (and (= port chan) (some? message))
                             (try
                               (f (get-in message [:data :resource]))
                               (catch Exception e
                                 (t/log! {:level :error :error e}
                                         "Message handler callback failed; skipping message")))
                             (recur)))))]
          (assoc this :running true :stop-chan stop-chan :worker worker)))))

  (stop [this]
    (if-not running
      (do
        (t/log! :warn (format "%s is not running" component-name))
        this)
      (do
        (t/log! :info (format "stopping %s" component-name))
        (a/close! (:stop-chan this))
        (assoc this :running false)))))

(defn make-handler
  "Creates a message handler that will apply the function `f` on messages incoming from `chan`."
  [chan f]
  (map->MessageHandler {:chan    chan
                        :f       f
                        :running false}))
