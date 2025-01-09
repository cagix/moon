(ns cdq.error
  (:require [clj-commons.pretty.repl :as pretty-repl]))

(defn pretty-pst [t]
  (binding [*print-level* 3]
    (pretty-repl/pretty-pst t 24)))
