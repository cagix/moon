(ns cdq.debug
  (:require [cdq.context :refer [grid-cell circle->cells]]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.math.shapes :refer [circle->outer-rectangle]]))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn render-before-entities [{:keys [gdl.context/world-viewport
                                      cdq.context/factions-iterations]
                               :as c}]
  (let [cam (:camera world-viewport)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (c/grid c
              (int left-x) (int bottom-y)
              (inc (int (:width  world-viewport)))
              (+ 2 (int (:height world-viewport)))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (grid-cell c [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (c/filled-rectangle c x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (c/filled-rectangle c x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (c/filled-rectangle c x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test [c]
  (let [position (c/world-mouse-position c)
        radius 0.8
        circle {:position position :radius radius}]
    (c/circle c position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells c circle))]
      (c/rectangle c x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (c/rectangle c x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [c]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (c/world-mouse-position c))
          cell (grid-cell c [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (c/rectangle c x y 1 1
                     (case (:movement @cell)
                       :air  [1 1 0 0.5]
                       :none [1 0 0 0.5]))))))

(defn render-after-entities [c]
  #_(geom-test c)
  (highlight-mouseover-tile c))
