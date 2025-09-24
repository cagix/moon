(ns cdq.string)

(defn ->edn-str [v]
  (binding [*print-level* nil]
    (pr-str v)))

(defn truncate [s limit]
  (if (> (count s) limit)
    (str (subs s 0 limit) "...")
    s))

(defmacro with-err-str [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))
