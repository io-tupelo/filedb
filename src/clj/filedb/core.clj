(ns filedb.core
  ; (:gen-class) ; uncomment to make runnable JAR file
  (:require
    [clojure.edn :as edn]
    [schema.core :as s]
    [tupelo.io :as tio]
    [tupelo.misc :as misc]
    )
  (:import
    [java.io File]
    [java.nio.file Path]
    [java.time Instant]
    ))

(s/defn java-time-instant-now :- Instant
  "Overridable version of java.time.Instant/now (for testability)"
  [] (Instant/now))

(s/defn write-file
  "Saves an EDN data structure to a HashFile"
  [file-spec :- (s/cond-pre s/Str File Path)
   data-value :- s/Any]
  (let [data--string (pr-str (misc/normalized-sorted data-value))
        data--hash   (misc/str->sha data--string)
        hashfile-map {:file/type    :hash/file
                      :file/format  :v20.03.28
                      :time/instant (Instant/now)
                      :hash/type    :hash/sha-1
                      :data/hash    data--hash
                      :data/type    :data/edn
                      :data/string  data--string}
        hashfile-str (pr-str hashfile-map)
        ]
    (spit (tio/->File file-spec) hashfile-str)))

(s/defn load-file
  "Loads and parses a HashFile"
  [file-spec :- (s/cond-pre s/Str File Path)]
  (let [hashfile-str  (slurp (tio/->File file-spec))
        hashfile-map  (edn/read-string hashfile-str)

        file--type    (fetch :file/type hashfile-map)
        file--format  (fetch :file/format hashfile-map)
        time--instant (Instant/parse (fetch :time/instant hashfile-map))
        hash--type    (fetch :hash/type hashfile-map)
        data--type    (fetch :data/type hashfile-map)
        data--hash    (fetch :data/hash hashfile-map) ; a String like "a9993e364706816aba3e25717850c26c9cd0d89d"
        data--string  (fetch :data/string hashfile-map) ; a String like "{:a 1 :b [2 3] }"

        >>            (assert (= file--type :hash/file))
        >>            (assert (= file--format :v20.03.28))
        >>            (assert (= hash--type :hash/sha-1))
        >>            (assert (= data--type :data/edn))
        >>            (assert (= data--hash (misc/str->sha data--string)))

        data-value    (edn/read-string data--string)
        ]
    ))

(defn -main []
  (println "main - enter")
  )


