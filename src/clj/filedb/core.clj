(ns filedb.core
  (:refer-clojure :exclude [load save])
  (:use tupelo.core)
  (:require
    [clojure.edn :as edn]
    [schema.core :as s]
    [tupelo.io :as tio]
    [tupelo.misc :as misc]
    [tupelo.schema :as tsk]
    [tupelo.string :as str])
  (:import
    [java.io File] ;[java.nio.file Path]
    [java.time Instant]
    [java.util UUID]
    ))

(s/def ^:dynamic *filedb-root-dir* :- s/Str
  "./filedb.d")

(s/defn java-time-instant-now :- Instant
  "Overridable version of java.time.Instant/now (for testability)"
  [] (Instant/now))

;---------------------------------------------------------------------------------------------------
(s/defn ^:no-doc lock-file :- File
  []
  (let [lock-file (File. (tio/->File *filedb-root-dir*) "lock.file")]
    (tio/mkdirs-parent lock-file)
    lock-file))

(def ^:no-doc token-unlocked (str "filedb.unlocked" \newline))

(s/defn ^:no-doc lock-release-force! :- nil
  []
  (spit (lock-file) token-unlocked)
  nil)

(s/defn ^:no-doc lock-release! :- nil
  [token :- s/Str]
  (let [token-in (slurp (lock-file))]
    (when-not (str/nonblank= token-in token)
      (throw (ex-info "Token lock error - release" (vals->map token token-in))))
    (spit (lock-file) token-unlocked)
    nil))

(s/defn ^:no-doc lock-acquire! :- nil
  [token :- s/Str]
  (let [token-out (str (str/trim token) \newline)
        token-in  (slurp (lock-file))]
    (when-not (str/nonblank= token-in token-unlocked)
      (throw (ex-info "Token lock error - acquire" (vals->map token token-in))))
    (spit (lock-file) token-out)
    nil))

(defn ^:no-doc with-file-lock-impl
  [forms]
  `(let [uuid-str# (str (UUID/randomUUID))]
     (try
       (lock-acquire! uuid-str#)
       ~@forms
       (finally
         (lock-release! uuid-str#)))))

(defmacro ^:no-doc with-file-lock
  "Evaluate `forms` using a file system lock under `*filedb-root-dir*` "
  [& forms]
  (with-file-lock-impl forms))

;---------------------------------------------------------------------------------------------------
(s/defn ^:no-doc build-hashfile-map :- tsk/KeyMap
  "Build a HashFile data map"
  [data-value :- s/Any]
  (let [data--string (pr-str (misc/normalized-sorted data-value))
        data--hash   (misc/str->sha data--string)
        hashfile-map {:file/type    :hash/file
                      :file/format  :v20.03.28
                      :time/instant (str (java-time-instant-now))
                      :hash/type    :hash/sha-1
                      :data/hash    data--hash
                      :data/type    :data/edn
                      :data/string  data--string}]
    hashfile-map))

(s/defn ^:no-doc parse-hashfile-map :- s/Any
  "Loads and parses a HashFile"
  [hashfile-map :- tsk/KeyMap]
  (let [file--type    (grab :file/type hashfile-map)
        file--format  (grab :file/format hashfile-map)
        time--instant (Instant/parse (grab :time/instant hashfile-map)) ; verify legal instant (can parse)
        hash--type    (grab :hash/type hashfile-map)
        data--type    (grab :data/type hashfile-map)
        data--hash    (grab :data/hash hashfile-map) ; a String like "a9993e364706816aba3e25717850c26c9cd0d89d"
        data--string  (grab :data/string hashfile-map) ; a String like "{:a 1 :b [2 3] }"

        >>            (assert (= file--type :hash/file))
        >>            (assert (= file--format :v20.03.28))
        >>            (assert (instance? Instant time--instant))
        >>            (assert (= hash--type :hash/sha-1))
        >>            (assert (= data--type :data/edn))
        >>            (assert (= data--hash (misc/str->sha data--string)))

        data-value    (edn/read-string data--string)]
    data-value))

(s/defn file-key->file-abs :- File
  [file-key :- s/Str]
  (let [filespec-rel-str (str (tio/->File (str file-key ".edn")))
        filespec-abs     (File. (tio/->File *filedb-root-dir*)
                           filespec-rel-str)]
    ; (spyx filespec-abs)
    filespec-abs))

(s/defn save
  "Saves an EDN data structure to a HashFile under the directory `*filedb-root-dir*`.
   The arg `file-key` is a unique String identifier which must be a legal relative directory path like:

         joe

         cust-joe
         cust-2019-joe

         cust.joe
         cust.2019.joe

         cust/joe
         cust/2019/joe

   where use of a file separator like `/` on Unix/OSX will result in nested directories.
  "
  [file-key :- s/Str
   data-value :- s/Any]
  (with-file-lock
    (let [filespec-abs (file-key->file-abs file-key)
          hashfile-str (pretty-str ; pr-str
                         (build-hashfile-map data-value))]
      (tio/mkdirs-parent filespec-abs)
      (spit filespec-abs hashfile-str))))

(s/defn load :- s/Any
  "Loads and parses an EDN data structure from a HashFile under the directory `*filedb-root-dir*`.
   The arg `file-key` is a unique String identifier. See filedb.core/save for examples. "
  [file-key :- s/Str]
  (with-file-lock
    (let [filespec-abs (file-key->file-abs file-key)
          hashfile-str (slurp filespec-abs)
          hashfile-map (edn/read-string hashfile-str)
          data-value   (parse-hashfile-map hashfile-map)]
      data-value)))


