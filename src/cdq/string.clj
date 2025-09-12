(ns cdq.string
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]))

(defn remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

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
