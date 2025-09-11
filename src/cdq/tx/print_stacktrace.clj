(ns cdq.tx.print-stacktrace
  (:require [cdq.stacktrace :as stacktrace]))

(defn do! [_ctx throwable]
  (stacktrace/pretty-print throwable)
  nil)
