(ns cdq.tx.set-movement
  (:require [cdq.entity :as entity]))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npc/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npc/player movement 'state' and no movement 'component' necessary !
; for projectiles inside projectile update !?
(defn- set-movement [entity movement-vector]
  (assoc entity :entity/movement {:direction movement-vector
                                  :speed (or (entity/stat entity :entity/movement-speed) 0)}))

(defn do! [eid movement-vector]
  (swap! eid set-movement movement-vector))
