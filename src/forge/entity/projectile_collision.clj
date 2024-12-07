(ns forge.entity.projectile-collision
  (:require [clojure.utils :refer [defmethods]]
            [forge.effects :refer [effects-do!]]
            [forge.entity :refer [->v tick]]
            [forge.entity.body :refer [e-collides?]]
            [forge.world.grid :refer [rectangle->cells
                                      cells->entities
                                      cell-blocked?]]))

(defmethods :entity/projectile-collision
  (v [[_ v]]
    (assoc v :already-hit-bodies #{}))

  (tick [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (rectangle->cells entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (e-collides? entity @%))
                                 (cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(cell-blocked? % (:z-order entity)) cells*))]
      (when destroy?
        (swap! eid assoc :entity/destroyed? true))
      (when hit-entity
        (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
      (when hit-entity
        (effects-do! {:effect/source eid
                      :effect/target hit-entity}
                     entity-effects)))))
