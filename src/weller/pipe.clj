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
            [weller.predicates :as pred]))

(def ^:const component-name "Pipe")

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

(defn add-filtered-tap
  "Adds a filtered (by `pred`) tap to the pipe. Filtered messages are processed by function `f`."
  [this pred f]
  (assoc this :taps (conj (:taps this) (mh/make-handler (pred/make-tap (:mult this) pred) f))))

(defn make-pipe
  "Creates a pipe with a built-in ActiveMQ listener.
  If a predicate `pred` and a function `f` are provided, then also add a filtered tap to it and start the pipe."
  ([]
   ;; load configuration
   (c/configure)
   (let [chan (a/chan)]
     (map->Pipe {:listener (activemq/make-listener (:activemq @c/config) chan)
                 :taps     []
                 :chan     chan
                 :mult     (a/mult chan)
                 :running  false})))
  ([pred f]
   (-> (make-pipe)
       (add-filtered-tap pred f)
       (component/start))))