(defproject gymheatmap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.11.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [seesaw "1.4.5"]]
  :main ^:skip-aot gymheatmap.core
  :target-path "target/%s"
  :resource-paths [ "local-jars/jheatchart-0.6.jar" "resources"] 
  :profiles {:uberjar {:aot :all}})
