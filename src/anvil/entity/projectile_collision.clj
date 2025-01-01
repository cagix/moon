(ns ^:no-doc anvil.entity.projectile-collision
  (:require [anvil.effect :as effect]
            [anvil.entity :as entity]
            [cdq.context :as world]
            [cdq.grid :as grid]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [find-first]]))

(defcomponent :entity/projectile-collision
  (component/->v [[_ v] c]
    (assoc v :already-hit-bodies #{}))

  (component/tick [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid c]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (world/rectangle->cells c entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (entity/collides? entity @%))
                                 (grid/cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(grid/blocked? % (:z-order entity)) cells*))]
      (when destroy?
        (swap! eid assoc :entity/destroyed? true))
      (when hit-entity
        (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
      (when hit-entity
        (effect/do-all! c
                        {:effect/source eid
                         :effect/target hit-entity}
                        entity-effects)))))
