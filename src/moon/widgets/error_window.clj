(ns moon.widgets.error-window
  (:require [clj-commons.pretty.repl :refer [pretty-pst]]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(defmacro ^:private with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn error-window! [throwable]
  (binding [*print-level* 5]
    (pretty-pst throwable 24))
  (stage/add!
   (ui/window {:title "Error"
               :rows [[(ui/label (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))
