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
  (:require [taoensso.telemere :as t]
            [weller.components.component :as component]
            [weller.events :as events]
            [weller.filters :as filters]
            [weller.handler :as handler])
  (:gen-class))

(defn -main
  [& args]

  (def handler (-> (handler/make-handler)
                   (handler/add-tap (filters/event? events/node-created) #(t/log! %))
                   (handler/add-tap (filters/event? events/node-created) #(t/log! %))
                   (component/start)))

  (Thread/sleep 5000)

  (component/stop handler))