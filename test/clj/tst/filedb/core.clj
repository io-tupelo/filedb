(ns tst.filedb.core
  (:use filedb.core tupelo.core tupelo.test)
  (:require
    [clojure.edn :as edn]
    [schema.core :as s]
    [tupelo.misc :as misc]
    [tupelo.string :as str])
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

(dotest
  (let [instant-party (Instant/parse "1999-12-31t23:59:59.123Z")
        ]
    (spyx instant-party)
    )



  )



