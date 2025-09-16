(ns cdq.tx.print-stacktrace
  (:require [clj-commons.pretty.repl :as pretty-repl]))

(def print-level 3)
(def print-depth 24)

(defn do! [_ctx throwable]
  (binding [*print-level* print-level]
    (pretty-repl/pretty-pst throwable print-depth))
  nil)
