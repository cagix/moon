(ns forge.entity.projectile-collision
  (:require [anvil.entity :as entity]
            [clojure.utils :refer [find-first]]
            [forge.effect :refer [effects-do!]]
            [forge.world.grid :refer [rectangle->cells
                                      cells->entities
                                      cell-blocked?]]))

(defn ->v [[_ v]]
  (assoc v :already-hit-bodies #{}))

(defn tick [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
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
                                     (entity/collides? entity @%))
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
                   entity-effects))))
