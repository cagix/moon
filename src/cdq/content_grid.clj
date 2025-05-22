(ns cdq.content-grid
  (:require [cdq.grid2d :as g2d]
            [gdl.tiled :as tiled]))

(defn create [tiled-map cell-size]
  (let [width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)]
    {:grid (g2d/create-grid
            (inc (int (/ width  cell-size))) ; inc because corners
            (inc (int (/ height cell-size)))
            (fn [idx]
              (atom {:idx idx,
                     :entities #{}})))
     :cell-w cell-size
     :cell-h cell-size}))

(defn- update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{:keys [cdq.content-grid/content-cell] :as entity} @eid
        [x y] (:position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc :cdq.content-grid/content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(def add-entity! update-entity!)

(defn remove-entity! [eid]
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
