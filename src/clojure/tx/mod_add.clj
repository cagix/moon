(ns clojure.tx.mod-add
  (:require [clojure.entity :as entity]
            [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers))
