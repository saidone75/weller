;  Weller is like Alfresco out-of-process extensions but 100% Clojure
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
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [weller.config :as c]
            [immuconf.config :as immu]
            [cral.api.auth :as auth]
            [taoensso.telemere :as t]))

(def config-file "resources/config.edn")

(defn ticket [f]
  (if (.exists (io/file config-file))
    ;; load configuration
    (let [config (immu/load config-file)]
      (reset! c/config config)
      (cral.config/configure (:alfresco config))
      (swap! c/config assoc :ticket (get-in (auth/create-ticket (get-in config [:alfresco :user]) (get-in config [:alfresco :password])) [:body :entry])))
    (t/log! :warn (format "unable to find %s, using defaults" config-file)))
  (f))