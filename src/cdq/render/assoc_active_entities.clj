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

(defn do! [{:keys [ctx/content-grid
                   ctx/player-eid]
            :as ctx}]
  (assoc ctx
         :ctx/active-entities
         (active-entities content-grid @player-eid)))
