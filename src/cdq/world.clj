(ns cdq.world)

(defprotocol Context
  (context-entity-add! [_ eid])
  (context-entity-remove! [_ eid])
  (context-entity-moved! [_ eid]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Grid
  (nearest-enemy-distance [_ entity])
  (nearest-enemy [_ entity])
  (potential-field-find-direction [_ eid]))

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol Creatures
  (spawn-creature! [_ {:keys [position creature-id components]}]))
