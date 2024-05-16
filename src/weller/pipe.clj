;  Weller is like Alfresco out-of-process extensions but in Clojure
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

(ns weller.pipe
  (:require [clojure.core.async :as a]
            [taoensso.telemere :as t]
            [weller.components.activemq-listener :as activemq]
            [weller.components.component :as component]
            [weller.components.message-handler :as mh]
            [weller.config :as c]
            [weller.filters :as filters]))

(def component-name "Pipe")

(defrecord Pipe
  [listener taps chan mult running]
  component/Component

  (start [this]
    (if running
      (do
        (t/log! :warn (format "%s is already running" component-name))
        this)
      (assoc this
        :listener (component/start (:listener this))
        :taps (doall (map component/start (:taps this)))
        :running true)))

  (stop [this]
    (if-not running
      (do
        (t/log! :warn (format "%s is not running" component-name))
        this)
      (assoc this
        :taps (doall (map component/stop (:taps this)))
        :listener (component/stop (:listener this))
        :running false))))

(defn add-tap [this pred f]
  (assoc this :taps (conj (:taps this) (mh/make-handler (filters/make-tap (:mult this) pred) f))))

(defn make-pipe
  ([]
   ;; load configuration
   (c/configure)
   (let [chan (a/chan)]
     (map->Pipe {:listener        (activemq/make-listener (:activemq @c/config) chan)
                         :taps    []
                         :chan    chan
                         :mult    (a/mult chan)
                         :running false})))
  ([pred f]
   (-> (make-pipe)
       (add-tap pred f)
       (component/start))))