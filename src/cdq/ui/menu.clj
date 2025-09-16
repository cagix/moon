(ns cdq.ui.menu
  (:import (clojure.lang MultiFn)))

(defn init! [ctx impls]
  (doseq [[defmulti-var k method-fn-var] impls]
    (assert (var? defmulti-var))
    (assert (keyword? k))
    (assert (var? method-fn-var))
    (MultiFn/.addMethod @defmulti-var k method-fn-var))
  ctx)
