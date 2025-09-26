(ns cdq.tx.print-stacktrace
  (:require [clj-commons.pretty.repl :as pretty-repl]))

(let [print-level 3
      print-depth 24]
  (defn do! [_ctx throwable]
    (binding [*print-level* print-level]
      (pretty-repl/pretty-pst throwable print-depth))
    nil))
