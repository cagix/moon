(ns clojure.tx.stack-item
  (:require [clojure.ctx.effect-handler :refer [do!]]))

; TODO no items which stack are available
#_(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (clojure.tx.remove-item/do! eid cell)
            (clojure.tx.set-item/do! eid cell (update cell-item :count + (:count item))))))

#_(defn do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))
