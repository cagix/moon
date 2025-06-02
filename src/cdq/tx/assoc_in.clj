(ns cdq.tx.assoc-in
  (:require [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value))
