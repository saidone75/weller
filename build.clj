;
; Weller - At your service. Out of process.
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

(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'org.saidone/weller)
(def version "0.1.1-SNAPSHOT")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def pom-file (format "%s/META-INF/maven/%s/%s/pom.xml" class-dir (namespace lib) (name lib)))
(def repository-url "https://github.com/saidone75/weller")
(def basis (delay (b/create-basis {:project "deps.edn" :user nil})))

(defn clean
  "Remove generated build artifacts."
  [_]
  (b/delete {:path "target"}))

(defn jar
  "Build the library source JAR and Maven POM."
  [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :src-pom :none
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]
                :resource-dirs ["resources"]
                :scm {:url repository-url
                      :connection (str "scm:git:" repository-url ".git")
                      :tag (b/git-process {:git-args ["rev-parse" "HEAD"]})}
                :pom-data [[:description "Alfresco out-of-process extensions in Clojure"]
                           [:url "https://saidone.org"]
                           [:licenses
                            [:license
                             [:name "GNU General Public License v3.0"]
                             [:url "https://www.gnu.org/licenses/gpl-3.0.txt"]]]]})
  (b/copy-dir {:src-dirs ["src" "resources"] :target-dir class-dir})
  (b/jar {:class-dir class-dir :jar-file jar-file}))

(defn install
  "Build and install the library into the local Maven repository."
  [_]
  (jar nil)
  (b/install {:basis @basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy
  "Build and publish to Clojars using CLOJARS_USERNAME and CLOJARS_PASSWORD (deploy token)."
  [_]
  (doseq [env-var ["CLOJARS_USERNAME" "CLOJARS_PASSWORD"]]
    (when (not (seq (System/getenv env-var)))
      (throw (ex-info (str "Missing environment variable: " env-var) {:env-var env-var}))))
  (jar nil)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   {:installer :remote
    :artifact jar-file
    :pom-file pom-file}))

(defn info
  "Print library coordinates for scripts."
  [_]
  (println lib version))

(defn uber
  "Build an AOT-compiled uberjar, matching the former Leiningen uberjar profile."
  [_]
  (jar nil)
  (b/compile-clj {:basis @basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :compile-opts {:direct-linking true}
                  :java-opts ["-Dclojure.spec.skip-macros=true"]})
  (b/uber {:class-dir class-dir
           :uber-file (format "target/%s-%s-standalone.jar" (name lib) version)
           :basis @basis
           :main 'weller.core}))
