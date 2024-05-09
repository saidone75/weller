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

(ns weller.core
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [cral.model.alfresco.cm :as cm]
            [immuconf.config :as immu]
            [taoensso.telemere :as t]
            [weller.components.application :as application]
            [weller.components.event-handler :as handler]
            [weller.components.activemq :as activemq]
            [weller.config :as c]
            [weller.events :as events]
            [weller.filters :as filters]
            [weller.system :as system])
  (:gen-class))

(defn- on-exit
  [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable f)))



(defn- shutdown
  []
  ;; stop system
  (component/stop (:system @c/state))
  (shutdown-agents))



(defn -main
  [& args]

  (let [system (system/make-system)]

    (system/add-handler (handler/make-handler (filters/make-filter (filters/event? events/node-created)) #(t/log! %)))
    (system/add-handler (handler/make-handler (filters/make-filter (filters/event? events/node-created)) #(t/log! %)))

    (system/start-system)

    (println system)

    )






  )