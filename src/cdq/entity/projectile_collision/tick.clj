(ns cdq.entity.projectile-collision.tick
  (:require [cdq.entity.body :as body]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [clojure.grid2d :as g2d]))

(defn txs
  [{:keys [entity-effects already-hit-bodies piercing?]}
   eid
   {:keys [world/grid]}]
  (let [entity @eid
        cells* (map deref (g2d/get-cells grid (body/touched-tiles (:entity/body entity))))
        hit-entity (first (filter #(and (not (contains? already-hit-bodies %))
                                        (not= (:entity/faction entity)
                                              (:entity/faction @%))
                                        (:body/collides? (:entity/body @%))
                                        (body/overlaps? (:entity/body entity)
                                                        (:entity/body @%)))
                                  (grid/cells->entities cells*)))
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
