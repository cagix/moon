(ns cdq.impl.content-grid
  (:require [cdq.entity :as entity]
            [cdq.grid2d :as g2d]))

(defn- update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{:keys [cdq.content-grid/content-cell] :as entity} @eid
        [x y] (entity/position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc :cdq.content-grid/content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(def add-entity! update-entity!)

(defn remove-entity! [_ eid]
  (-> @eid
      :cdq.content-grid/content-cell
      (swap! update :entities disj eid)))

(def position-changed! update-entity!)

(defn active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))
