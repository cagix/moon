(ns clojure.modules
  (:require [clojure.mapgen :refer [grid->tiled-map transition-idx-value]]
            [clojure.tiled :as tiled]))

; * unique max 16 modules, not random take @ #'floor->module-index, also special start, end modules, rare modules...
; * at the beginning enemies very close, different area different spawn-rate !
; beginning slow enemies low hp low dmg etc.
; * flood-fill gets 8 neighbour posis -> no NADs on modules ! assert !
; * assuming bottom left in floor module is walkable
; whats the assumption here? => or put extra borders around? / assert!

(def file "maps/modules.tmx")
(def width  32)
(def height 20)
(def scale [width height])

(def ^:private number-modules-x 8)
(def ^:private number-modules-y 4)
(def ^:private module-offset-tiles 1)
(def ^:private transition-modules-row-width 4)
(def ^:private transition-modules-row-height 4)
(def ^:private transition-modules-offset-x 4)
(def ^:private floor-modules-row-width 4)
(def ^:private floor-modules-row-height 4)
(def ^:private floor-idxvalue 0)

(defn- module-index->tiled-map-positions [[module-x module-y]]
  (let [start-x (* module-x (+ width  module-offset-tiles))
        start-y (* module-y (+ height module-offset-tiles))]
    (for [x (range start-x (+ start-x width))
          y (range start-y (+ start-y height))]
      [x y])))

(defn- floor->module-index []
  [(rand-int floor-modules-row-width)
   (rand-int floor-modules-row-height)])

(defn- transition-idxvalue->module-index [idxvalue]
  [(+ (rem idxvalue transition-modules-row-width)
      transition-modules-offset-x)
   (int (/ idxvalue transition-modules-row-height))])

(defn- place-module [scaled-grid
                     unscaled-position
                     & {:keys [transition?
                               transition-neighbor?]}]
  (let [idxvalue (if transition?
                   (transition-idx-value unscaled-position transition-neighbor?)
                   floor-idxvalue)
        tiled-map-positions (module-index->tiled-map-positions
                             (if transition?
                               (transition-idxvalue->module-index idxvalue)
                               (floor->module-index)))
        offsets (for [x (range width)
                      y (range height)]
                  [x y])
        offset->tiled-map-position (zipmap offsets tiled-map-positions)
        scaled-position (mapv * unscaled-position scale)]
    (reduce (fn [grid offset]
              (assoc grid
                     (mapv + scaled-position offset)
                     (offset->tiled-map-position offset)))
            scaled-grid
            offsets)))

(defn place [modules-tiled-map
             scaled-grid
             unscaled-grid
             unscaled-floor-positions
             unscaled-transition-positions]
  (let [_ (assert (and (= (tiled/tm-width modules-tiled-map)
                          (* number-modules-x (+ width module-offset-tiles)))
                       (= (tiled/tm-height modules-tiled-map)
                          (* number-modules-y (+ height module-offset-tiles)))))
        scaled-grid (reduce (fn [scaled-grid unscaled-position]
                              (place-module scaled-grid unscaled-position :transition? false))
                            scaled-grid
                            unscaled-floor-positions)
        scaled-grid (reduce (fn [scaled-grid unscaled-position]
                              (place-module scaled-grid unscaled-position :transition? true
                                            :transition-neighbor? #(#{:transition :wall}
                                                                    (get unscaled-grid %))))
                            scaled-grid
                            unscaled-transition-positions)]
    (grid->tiled-map modules-tiled-map scaled-grid)))
