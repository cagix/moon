(ns cdq.world-fns.modules
  (:require [cdq.area-level-grid :as area-level-grid]
            [cdq.grid2d :as g2d]
            [cdq.level.helper :refer [prepare-creature-properties
                                      add-creatures-layer!
                                      scale-grid
                                      cave-grid
                                      adjacent-wall-positions
                                      flood-fill
                                      grid->tiled-map
                                      transition-idx-value]]
            [clojure.gdx.maps.tiled :as tiled]))

(def modules-file "maps/modules.tmx") ; used @ tst
(def modules-width  32) ; usd @ test
(def modules-height 20) ; used @ test
(def modules-scale [modules-width modules-height])

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

(defn- place-module* [scaled-grid
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

(defn place-module [modules-tiled-map
                    scaled-grid
                    unscaled-grid
                    unscaled-floor-positions
                    unscaled-transition-positions]
  (let [_ (assert (and (= (:tiled-map/width modules-tiled-map)
                          (* number-modules-x (+ modules-width module-offset-tiles)))
                       (= (:tiled-map/height modules-tiled-map)
                          (* number-modules-y (+ modules-height module-offset-tiles)))))
        scaled-grid (reduce (fn [scaled-grid unscaled-position]
                              (place-module* scaled-grid unscaled-position :transition? false))
                            scaled-grid
                            unscaled-floor-positions)
        scaled-grid (reduce (fn [scaled-grid unscaled-position]
                              (place-module* scaled-grid unscaled-position :transition? true
                                            :transition-neighbor? #(#{:transition :wall}
                                                                    (get unscaled-grid %))))
                            scaled-grid
                            unscaled-transition-positions)]
    (grid->tiled-map modules-tiled-map scaled-grid)))

; * unique max 16 modules, not random take @ #'floor->module-index, also special start, end modules, rare modules...
; * at the beginning enemies very close, different area different spawn-rate !
; beginning slow enemies low hp low dmg etc.
; * flood-fill gets 8 neighbour posis -> no NADs on modules ! assert !
; * assuming bottom left in floor module is walkable
; whats the assumption here? => or put extra borders around? / assert!

(defn- generate-modules
  "The generated tiled-map needs to be disposed."
  [{:keys [world/map-size
           world/max-area-level
           world/spawn-rate]}
   creature-properties]
  (assert (<= max-area-level map-size))
  (let [{:keys [start grid]} (cave-grid :size map-size)
        ;_ (printgrid grid)
        ;_ (println " - ")
        grid (reduce #(assoc %1 %2 :transition) grid (adjacent-wall-positions grid))
        ;_ (printgrid grid)
        ;_ (println " - ")
        _ (assert (or
                   (= #{:wall :ground :transition} (set (g2d/cells grid)))
                   (= #{:ground :transition} (set (g2d/cells grid))))
                  (str "(set (g2d/cells grid)): " (set (g2d/cells grid))))
        scale modules-scale
        scaled-grid (scale-grid grid scale)
        tiled-map (place-module (tiled/tmx-tiled-map modules-file)
                                 scaled-grid
                                 grid
                                 (filter #(= :ground     (get grid %)) (g2d/posis grid))
                                 (filter #(= :transition (get grid %)) (g2d/posis grid)))
        start-position (mapv * start scale)
        can-spawn? #(= "all" (tiled/movement-property tiled-map %))
        _ (assert (can-spawn? start-position)) ; assuming hoping bottom left is movable
        spawn-positions (flood-fill scaled-grid start-position can-spawn?)
        ;_ (println "scaled grid with filled nil: '?' \n")
        ;_ (printgrid (reduce #(assoc %1 %2 nil) scaled-grid spawn-positions))
        ;_ (println "\n")
        {:keys [steps area-level-grid]} (area-level-grid/create
                                         :grid grid
                                         :start start
                                         :max-level max-area-level
                                         :walk-on #{:ground :transition})
        ;_ (printgrid area-level-grid)
        _ (assert (or
                   (= (set (concat [max-area-level] (range max-area-level)))
                      (set (g2d/cells area-level-grid)))
                   (= (set (concat [:wall max-area-level] (range max-area-level)))
                      (set (g2d/cells area-level-grid)))))
        scaled-area-level-grid (scale-grid area-level-grid scale)
        get-free-position-in-area-level (fn [area-level]
                                          (rand-nth
                                           (filter
                                            (fn [p]
                                              (and (= area-level (get scaled-area-level-grid p))
                                                   (#{:no-cell :undefined}
                                                    (tiled/property-value (tiled/get-layer tiled-map "creatures")
                                                                          p
                                                                          "id"))))
                                            spawn-positions)))
        creatures (for [position spawn-positions
                        :let [area-level (get scaled-area-level-grid position)
                              creatures (filter #(= area-level (:creature/level %))
                                                creature-properties)]
                        :when (and (number? area-level)
                                   (<= (rand) spawn-rate)
                                   (seq creatures))]
                    [position (rand-nth creatures)])]
    (add-creatures-layer! tiled-map creatures)
    {:tiled-map tiled-map
     :start-position (get-free-position-in-area-level 0)
     :area-level-grid scaled-area-level-grid}))

(defn create
  [{:keys [creature-properties
           textures]
    :as params}]
  (generate-modules params
                    (prepare-creature-properties creature-properties textures)))
