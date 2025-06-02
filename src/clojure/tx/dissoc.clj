(ns clojure.tx.dissoc
  (:require [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k))
