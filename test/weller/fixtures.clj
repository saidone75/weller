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

(ns weller.fixtures
  (:require [clojure.test :refer :all]
            [cral.api.auth :as auth]
            [weller.config :as c]))

(defn ticket [f]
  ;; load configuration
  (c/configure)
  ;; configure CRAL
  (cral.config/configure (:alfresco @c/config))
  ;; put an Alfresco ticket in config atom later use
  (swap! c/config assoc :ticket (get-in (auth/create-ticket (get-in @c/config [:alfresco :user]) (get-in @c/config [:alfresco :password])) [:body :entry]))
  (f))