(ns forge.world.content-grid
  (:require [clojure.gdx.tiled :as tiled]
            [clojure.utils :refer [bind-root]]
            [data.grid2d :as g2d]))

(defn- create-grid [{:keys [cell-size width height]}]
  {:grid (g2d/create-grid
          (inc (int (/ width  cell-size))) ; inc because corners
          (inc (int (/ height cell-size)))
          (fn [idx]
            (atom {:idx idx,
                   :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(declare ^:private content-grid)

(defn init [tiled-map]
  (let [width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)]
    (bind-root content-grid (create-grid {:cell-size 16  ; FIXME global config
                                          :width  width
                                          :height height}))))

(defn add-entity [eid]
  (let [{:keys [grid cell-w cell-h]} content-grid
        {::keys [content-cell] :as entity} @eid
        [x y] (:position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc ::content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(defn remove-entity [eid]
  (-> @eid
      ::content-cell
      (swap! update :entities disj eid)))

(def entity-position-changed add-entity)

(defn active-entities [center-entity]
  (let [{:keys [grid]} content-grid]
    (->> (let [idx (-> center-entity
                       ::content-cell
                       deref
                       :idx)]
           (cons idx (g2d/get-8-neighbour-positions idx)))
         (keep grid)
         (mapcat (comp :entities deref)))))
