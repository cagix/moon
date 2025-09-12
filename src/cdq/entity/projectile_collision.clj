(ns cdq.entity.projectile-collision
  (:require [cdq.entity.body :as body]
            [cdq.grid2d :as g2d]
            [cdq.world.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.utils :as utils]
            [cdq.gdx.math.geom :as geom]))

(defn create [v _ctx]
  (assoc v :already-hit-bodies #{}))

(defn tick!
  [{:keys [entity-effects already-hit-bodies piercing?]}
   eid
   {:keys [ctx/grid]}]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (g2d/get-cells grid (geom/body->touched-tiles (:entity/body entity)))) ; just use cached-touched -cells
        hit-entity (utils/find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                           (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                                 (:entity/faction @%))
                                           (:body/collides? (:entity/body @%))
                                           (body/overlaps? (:entity/body entity)
                                                           (:entity/body @%)))
                                     (grid/cells->entities grid cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(cell/blocked? % (:body/z-order (:entity/body entity))) cells*))]
    [(when destroy?
       [:tx/mark-destroyed eid])
     (when hit-entity
       [:tx/assoc-in eid [:entity/projectile-collision :already-hit-bodies] (conj already-hit-bodies hit-entity)] ; this is only necessary in case of not piercing ...
       )
     (when hit-entity
       [:tx/effect {:effect/source eid :effect/target hit-entity} entity-effects])]))
