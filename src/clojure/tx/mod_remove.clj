(ns clojure.tx.mod-remove
  (:require [clojure.entity :as entity]
            [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers))
