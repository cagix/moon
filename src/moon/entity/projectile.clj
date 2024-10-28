(ns moon.entity.projectile
  (:require [gdl.utils :refer [find-first]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.world.grid :as grid]))

(defc :entity/projectile-collision
  {:let {:keys [entity-effects already-hit-bodies piercing?]}}
  (entity/->v [[_ v]]
    (assoc v :already-hit-bodies #{}))

  ; TODO probably belongs to body
  (entity/tick [[k _] eid]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (grid/rectangle->cells entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (entity/collides? entity @%))
                                 (grid/cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(grid/blocked? % (:z-order entity)) cells*))]
      [(when hit-entity
         [:e/assoc-in eid [k :already-hit-bodies] (conj already-hit-bodies hit-entity)]) ; this is only necessary in case of not piercing ...
       (when destroy?
         [:e/destroy eid])
       (when hit-entity
         [:tx/effect {:effect/source eid :effect/target hit-entity} entity-effects])])))
