(ns clojure.tx.assoc
  (:require [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value))
