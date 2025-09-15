(ns cdq.entity.state)

(defprotocol State
  (create       [_ eid ctx])
  (handle-input [_ eid ctx])
  (cursor       [_ eid ctx])
  (enter        [_ eid])
  (exit         [_ eid ctx])
  (clicked-inventory-cell [_ eid cell]))
