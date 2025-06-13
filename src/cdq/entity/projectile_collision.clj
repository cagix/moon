(ns cdq.entity.projectile-collision
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.utils :refer [defmethods find-first]]))

(defmethods :entity/projectile-collision
  (entity/create [[_ v] _ctx]
    (assoc v :already-hit-bodies #{}))

  (entity/tick! [[k {:keys [entity-effects already-hit-bodies piercing?]}]
                 eid
                 {:keys [ctx/grid]}]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (grid/rectangle->cells grid entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (entity/faction @%))
                                       (:collides? @%)
                                       (entity/overlaps? entity @%))
                                 (grid/cells->entities grid cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(cell/blocked? % (:z-order entity)) cells*))]
      [(when destroy?
         [:tx/mark-destroyed eid])
       (when hit-entity
         [:tx/assoc-in eid [k :already-hit-bodies] (conj already-hit-bodies hit-entity)] ; this is only necessary in case of not piercing ...
         )
       (when hit-entity
         [:tx/effect {:effect/source eid :effect/target hit-entity} entity-effects])])))
