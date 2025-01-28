(ns cdq.create.content-grid
  (:require [cdq.context :as context]
            [clojure.data.grid2d :as g2d]
            [clojure.gdx.tiled :as tiled]
            [clojure.utils :refer [defcomponent]]))

(defn- create* [{:keys [cell-size width height]}]
  {:grid (g2d/create-grid
          (inc (int (/ width  cell-size))) ; inc because corners
          (inc (int (/ height cell-size)))
          (fn [idx]
            (atom {:idx idx,
                   :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defn create [tiled-map]
  (create* {:cell-size 16
            :width  (tiled/tm-width  tiled-map)
            :height (tiled/tm-height tiled-map)}))

(defcomponent :cdq.context/content-grid
  (context/add-entity [[_ {:keys [grid cell-w cell-h]}] eid]
    (let [{:keys [cdq.content-grid/content-cell] :as entity} @eid
          [x y] (:position entity)
          new-cell (get grid [(int (/ x cell-w))
                              (int (/ y cell-h))])]
      (when-not (= content-cell new-cell)
        (swap! new-cell update :entities conj eid)
        (swap! eid assoc :cdq.content-grid/content-cell new-cell)
        (when content-cell
          (swap! content-cell update :entities disj eid)))))

  (context/remove-entity [_ eid]
    (-> @eid
        :cdq.content-grid/content-cell
        (swap! update :entities disj eid)))

  (context/position-changed [this eid]
    (context/add-entity this eid)))
