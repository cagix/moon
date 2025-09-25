(ns cdq.entity.state)

(defprotocol State
  (create       [_ eid world])
  (handle-input [_ eid ctx])
  (cursor       [_ eid ctx])
  (enter        [_ eid])
  (exit         [_ eid ctx])
  (clicked-inventory-cell [_ eid cell])
  (draw-gui-view [_ eid ctx]))
