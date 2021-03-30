(defproject io.tupelo/filedb "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [clojure.java-time "0.3.2"]
                 [http-kit "2.5.3"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.flatland/ordered "1.5.9"]
                 [prismatic/schema "1.1.12"]
                 [semantic-csv "0.2.1-alpha1"]
                 [tupelo "21.03.25"]
                 ]
  :plugins [[com.jakemccrary/lein-test-refresh "0.24.1"]
            [lein-ancient "0.7.0"]
            [lein-codox "0.10.7"]
            ]

  :profiles {:dev     {:dependencies []}
             :uberjar {:aot :all}}

  :global-vars {*warn-on-reflection* false}

  :main filedb.core
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clj"]
  :target-path "target/%s"
  :compile-path "%s/class-files"
  :clean-targets [:target-path]

  :jvm-opts ["-Xms500m" "-Xmx4g"]
  )

;---------------------------------------------------------------------------------------------------
(comment
  (do
    (require '[clojure.java.browse :as cjb])
    (dotest
      (spyx (cjb/browse-url "http://yahoo.com")))))

