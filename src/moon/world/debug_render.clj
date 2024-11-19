(ns ^:no-doc moon.world.debug-render
  (:require [gdl.graphics.camera :as cam]
            [gdl.graphics.world-view :as world-view]
            [gdl.math.shape :as shape]
            [gdl.utils :refer [->tile]]
            [moon.core :refer [draw-circle draw-rectangle draw-filled-rectangle draw-grid]]
            [moon.world.grid :refer [circle->cells grid]]
            [moon.world.potential-fields :refer [factions-iterations]]))

(defn- geom-test []
  (let [position (world-view/mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (draw-circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells circle))]
      (draw-rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (shape/circle->outer-rectangle circle)]
      (draw-rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug []
  (let [cam (world-view/camera)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (draw-grid (int left-x) (int bottom-y)
                 (inc (int (world-view/width)))
                 (+ 2 (int (world-view/height)))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (draw-filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (draw-filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (draw-filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (->tile (world-view/mouse-position))
          cell (get grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (draw-rectangle x y 1 1
                        (case (:movement @cell)
                          :air  [1 1 0 0.5]
                          :none [1 0 0 0.5]))))))

(defn before-entities []
  (tile-debug))

(defn after-entities []
  #_(geom-test)
  (highlight-mouseover-tile))
