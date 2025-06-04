(ns cdq.render.assoc-active-entities
  (:require [cdq.grid2d :as g2d]))

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn calculate-active-entities [{:keys [ctx/content-grid
                                         ctx/player-eid]}]
  (active-entities content-grid @player-eid))
