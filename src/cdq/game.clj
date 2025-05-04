(ns cdq.game
  (:require [cdq.data.grid2d :as g2d]))

(defn- active-entities* [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn get-active-entities
  "Expensive operation.

  Active entities are those which are nearby the position of the player and about one screen away."
  [{:keys [cdq.context/content-grid
           cdq.context/player-eid]}]
  (active-entities* content-grid @player-eid))
