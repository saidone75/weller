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

(defproject weller "0.1.0-SNAPSHOT"
  :description "Alfresco out-of-process extensions in Clojure"
  :url "https://saidone.org"
  :license {:name "GNU General Public License v3.0"
            :url  "https://www.gnu.org/licenses/gpl-3.0.txt"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.stuartsierra/component "1.1.0"]
                 [org.apache.activemq/activemq-broker "6.1.1"]
                 [org.clojure/data.json "2.5.0"]
                 [org.saidone/cral "0.3.1"]
                 [com.taoensso/telemere "1.0.0-beta9"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [org.clojure/core.async "1.6.681"]]
  :main ^:skip-aot weller.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
