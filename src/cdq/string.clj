(ns cdq.string
  (:require [clojure.pprint :as pprint]))

(defn ->edn-str [v]
  (binding [*print-level* nil]
    (pr-str v)))

(defn truncate [s limit]
  (if (> (count s) limit)
    (str (subs s 0 limit) "...")
    s))

(defn pprint-to-str [data & {:keys [print-level]}]
  (binding [*print-level* (or print-level 3)]
    (with-out-str
     (clojure.pprint/pprint data))))

(defmacro with-err-str [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))
