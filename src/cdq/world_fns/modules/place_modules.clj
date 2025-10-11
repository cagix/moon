(ns cdq.world-fns.modules.place-modules
  (:require [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.grid2d :as g2d]
            [clojure.grid2d.utils :as helper]))

(def ^:private number-modules-x 8)
(def ^:private number-modules-y 4)
(def ^:private module-offset-tiles 1)
(def ^:private transition-modules-row-width 4)
(def ^:private transition-modules-row-height 4)
(def ^:private transition-modules-offset-x 4)
(def ^:private floor-modules-row-width 4)
(def ^:private floor-modules-row-height 4)
(def ^:private floor-idxvalue 0)

(defn- module-index->tiled-map-positions
  [[module-x module-y]
   [modules-width modules-height]]
  (let [start-x (* module-x (+ modules-width  module-offset-tiles))
        start-y (* module-y (+ modules-height module-offset-tiles))]
    (for [x (range start-x (+ start-x modules-width))
          y (range start-y (+ start-y modules-height))]
      [x y])))

(defn- floor->module-index []
  [(rand-int floor-modules-row-width)
   (rand-int floor-modules-row-height)])

(defn- transition-idxvalue->module-index [idxvalue]
  [(+ (rem idxvalue transition-modules-row-width)
      transition-modules-offset-x)
   (int (/ idxvalue transition-modules-row-height))])

(defn- place-module* [modules-scale
                      scaled-grid
                      unscaled-position
                      & {:keys [transition?
                                transition-neighbor?]}]
  (let [[modules-width modules-height] modules-scale
        idxvalue (if transition?
                   (helper/transition-idx-value unscaled-position transition-neighbor?)
                   floor-idxvalue)
        tiled-map-positions (module-index->tiled-map-positions
                             (if transition?
                               (transition-idxvalue->module-index idxvalue)
                               (floor->module-index))
                             modules-scale)
        offsets (for [x (range modules-width)
                      y (range modules-height)]
                  [x y])
        offset->tiled-map-position (zipmap offsets tiled-map-positions)
        scaled-position (mapv * unscaled-position modules-scale)]
    (reduce (fn [grid offset]
              (assoc grid
                     (mapv + scaled-position offset)
                     (offset->tiled-map-position offset)))
            scaled-grid
            offsets)))

(defn- place-modules
  [modules-tiled-map
   modules-scale
   scaled-grid
   unscaled-grid
   unscaled-floor-positions
   unscaled-transition-positions]
  (let [[modules-width modules-height] modules-scale
        _ (assert (and (= (.get (tiled-map/properties modules-tiled-map) "width")
                          (* number-modules-x (+ modules-width module-offset-tiles)))
                       (= (.get (tiled-map/properties modules-tiled-map) "height")
                          (* number-modules-y (+ modules-height module-offset-tiles)))))
        scaled-grid (reduce (fn [scaled-grid unscaled-position]
                              (place-module* modules-scale
                                             scaled-grid
                                             unscaled-position
                                             :transition? false))
                            scaled-grid
                            unscaled-floor-positions)
        scaled-grid (reduce (fn [scaled-grid unscaled-position]
                              (place-module* modules-scale
                                             scaled-grid
                                             unscaled-position
                                             :transition? true
                                             :transition-neighbor? #(#{:transition :wall}
                                                                     (get unscaled-grid %))))
                            scaled-grid
                            unscaled-transition-positions)]
    scaled-grid))

(defn do!
  [{:keys [scale
           scaled-grid
           grid
           schema-tiled-map]
    :as w}]
  (assoc w :scaled-grid (place-modules schema-tiled-map
                                       scale
                                       scaled-grid
                                       grid
                                       (filter #(= :ground     (get grid %)) (g2d/posis grid))
                                       (filter #(= :transition (get grid %)) (g2d/posis grid)))))
