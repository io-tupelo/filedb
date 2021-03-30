(ns tst.filedb.core
  (:refer-clojure :exclude [load save ])
  (:use filedb.core tupelo.core tupelo.test)
  (:require
    [schema.core :as s]
    [tupelo.misc :as misc]
    [tupelo.string :as str]
    [tupelo.io :as tio]
    [tupelo.schema :as tsk])
  (:import
    [java.time Instant]
    ))

(comment ; sample HashFile
  {:file/type    :hash/file
   :file/format  :v20.03.28
   :time/instant "2021-03-28t11:55:45.3Z"
   :hash/type    :hash/sha-1
   :data/hash    "a9993e364706816aba3e25717850c26c9cd0d89d"
   :data/type    :data/edn
   :data/string  "{:a 1 :b [2 3] }" ; an EDN string as from `pr` (notice the double-quotes). NOT an EDN data structure
   })

(def instant-party (Instant/parse "1999-12-31t23:44:55.666Z"))

(dotest
  (let [abc3          "abc"
        abc5          (pr-str abc3)] ; adds double-quotes => 5 chars total
    (is= 3 (count abc3))
    (is= 5 (count abc5))
    (is= (misc/str->sha abc3) "a9993e364706816aba3e25717850c26c9cd0d89d")
    (is= (misc/str->sha abc5) "b87f4bf9b7b07f594430548b653b4998e4b40402")))

(dotest
  (with-redefs [java-time-instant-now (const-fn instant-party)]
    (is= instant-party (java-time-instant-now))
    (let [result (it-> (build-hashfile-map "abc")
                   (update-in it [:data/string] str/quotes->single)
                   (update-in it [:time/instant] str))]
      (is= result {:file/type    :hash/file,
                   :file/format  :v20.03.28,
                   :time/instant "1999-12-31T23:44:55.666Z",
                   :hash/type    :hash/sha-1,
                   :data/hash    "b87f4bf9b7b07f594430548b653b4998e4b40402",
                   :data/type    :data/edn,
                   :data/string  "'abc'"}))))

(defn verify-hashfile-map-round-trip
  [arg]
  (let [hf-map (build-hashfile-map arg)
        >>     (s/validate tsk/KeyMap hf-map)
        result (parse-hashfile-map hf-map)]
    (is= result arg)))

(dotest
  (verify-hashfile-map-round-trip 5)
  (verify-hashfile-map-round-trip "abc")
  (verify-hashfile-map-round-trip [1 2 3])
  (verify-hashfile-map-round-trip {:a 1 :b [2 3 4]})
  (verify-hashfile-map-round-trip {:a 1 :b [9 "hello"]})
  (verify-hashfile-map-round-trip {:a 1 :b [9 "hello" #{9 8 7 2}]}))

(defn verify-hashfile-round-trip
  [arg]
  (let [fname (format "some/tree/dir/struct/tmp--%d" (System/nanoTime))]
    (save fname arg)
    (let [result (load fname)]
      (is= result arg))))

(dotest
  (let [tmpdir (tio/create-temp-directory "filedb")]
    (binding [*filedb-root-dir* tmpdir]
      (verify-hashfile-round-trip 5)
      (verify-hashfile-round-trip "abc")
      (verify-hashfile-round-trip [1 2 3])
      (verify-hashfile-round-trip {:a 1 :b [2 3 4]})
      (verify-hashfile-round-trip {:a 1 :b [9 "hello"]})
      (verify-hashfile-round-trip {:a 1 :b [9 "hello" #{9 8 7 2}]}))))

(dotest
  ; (binding [*filedb-root-dir* (tio/->File "./filedb-tmp-1519")])

  (verify-hashfile-round-trip 5)
  (verify-hashfile-round-trip "abc")
  (verify-hashfile-round-trip [1 2 3])
  (verify-hashfile-round-trip {:a 1 :b [2 3 4]})
  (verify-hashfile-round-trip {:a 1 :b [9 "hello"]})
  (verify-hashfile-round-trip {:a 1 :b [9 "hello" #{9 8 7 2}]}))




