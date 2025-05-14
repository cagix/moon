(ns cdq.game.cache-active-entities
  (:require [cdq.ctx :as ctx]
            [cdq.grid2d :as g2d]))

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn do! []
  (alter-var-root #'ctx/world
                  assoc
                  :active-entities
                  (active-entities (:content-grid ctx/world)
                                   @ctx/player-eid)))
