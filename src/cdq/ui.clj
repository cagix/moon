(ns cdq.ui
  (:require [gdl.ui :as ui]))

(defmacro ^:private with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn error-window [throwable]
  (ui/window {:title "Error"
              :rows [[(ui/label (binding [*print-level* 3]
                                  (with-err-str
                                    (clojure.repl/pst throwable))))]]
              :modal? true
              :close-button? true
              :close-on-escape? true
              :center? true
              :pack? true}))
