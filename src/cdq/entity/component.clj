(ns cdq.entity.component)

(defprotocol Component
  (create!  [_ eid ctx])
  (destroy! [_ eid ctx])
  (tick!    [_ eid ctx])
  (draw!    [_ entity ctx]))
