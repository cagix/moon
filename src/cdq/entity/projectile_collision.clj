(ns cdq.entity.projectile-collision
  (:require [cdq.entity.body :as body]
            [cdq.grid2d :as g2d]
            [cdq.world.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.gdx.math.geom :as geom]))

(defn create [v _ctx]
  (assoc v :already-hit-bodies #{}))

(defn tick!
  [{:keys [entity-effects already-hit-bodies piercing?]}
   eid
   {:keys [ctx/world]}]
  (let [grid (:world/grid world)
        entity @eid
        cells* (map deref (g2d/get-cells grid (geom/body->touched-tiles (:entity/body entity))))
        hit-entity (first (filter #(and (not (contains? already-hit-bodies %))
                                        (not= (:entity/faction entity)
                                              (:entity/faction @%))
                                        (:body/collides? (:entity/body @%))
                                        (body/overlaps? (:entity/body entity)
                                                        (:entity/body @%)))
                                  (grid/cells->entities grid cells*)))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(cell/blocked? % (:body/z-order (:entity/body entity))) cells*))]
    [(when destroy?
       [:tx/mark-destroyed eid])
     (when hit-entity
       [:tx/assoc-in
        eid
        [:entity/projectile-collision
         :already-hit-bodies]
        (conj already-hit-bodies hit-entity)])
     (when hit-entity
       [:tx/effect
        {:effect/source eid
         :effect/target hit-entity}
        entity-effects])]))
