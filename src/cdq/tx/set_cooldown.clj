(ns cdq.tx.set-cooldown
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.timer :as timer]))

(defmethod do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/elapsed-time]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create elapsed-time (:skill/cooldown skill))))
