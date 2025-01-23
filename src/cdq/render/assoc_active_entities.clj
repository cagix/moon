(ns cdq.render.assoc-active-entities
  (:require [clojure.data.grid2d :as g2d]))

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn render [{:keys [cdq.context/content-grid
                      cdq.context/player-eid]
               :as context}]
  (assoc context :cdq.game/active-entities (active-entities content-grid @player-eid)))
