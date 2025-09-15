(ns cdq.start.provide-impls
  (:require clojure.provide))

(defn do! [ctx impls]
  (clojure.provide/do! impls)
  ctx)
