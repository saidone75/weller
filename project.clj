(defproject weller "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.stuartsierra/component "1.1.0"]
                 [org.apache.activemq/activemq-broker "6.1.1"]
                 [org.clojure/data.json "2.5.0"]
                 [org.saidone/cral "0.3.0"]
                 [com.taoensso/telemere "1.0.0-beta9"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [org.clojure/core.async "1.6.681"]]
  :main ^:skip-aot weller.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
