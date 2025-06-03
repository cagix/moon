(ns clojure.content-grid
  (:require [clojure.entity :as entity]
            [clojure.grid2d :as g2d]))

(defn create [width height cell-size]
  {:grid (g2d/create-grid
          (inc (int (/ width  cell-size))) ; inc because corners
          (inc (int (/ height cell-size)))
          (fn [idx]
            (atom {:idx idx,
                   :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defn- update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{:keys [clojure.content-grid/content-cell] :as entity} @eid
        [x y] (entity/position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc :clojure.content-grid/content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(def add-entity! update-entity!)

(defn remove-entity! [eid]
  (-> @eid
      :clojure.content-grid/content-cell
      (swap! update :entities disj eid)))

(def position-changed! update-entity!)
