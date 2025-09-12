(ns cdq.world-fns.modules
  (:require [cdq.grid2d :as g2d]
            [cdq.world-fns.helper :as helper]
            [cdq.world-fns.area-level-grid :as area-level-grid]
            [cdq.world-fns.module :as module]
            [cdq.world-fns.creature-tiles :as creature-tiles]
            [cdq.world-fns.creature-layer :as creature-layer]
            [clojure.gdx.maps.tiled :as tiled]))

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
  (let [{:keys [start grid]} (helper/cave-grid :size map-size)
        ;_ (printgrid grid)
        ;_ (println " - ")
        grid (reduce #(assoc %1 %2 :transition) grid (helper/adjacent-wall-positions grid))
        ;_ (printgrid grid)
        ;_ (println " - ")
        _ (assert (or
                   (= #{:wall :ground :transition} (set (g2d/cells grid)))
                   (= #{:ground :transition} (set (g2d/cells grid))))
                  (str "(set (g2d/cells grid)): " (set (g2d/cells grid))))
        scale module/modules-scale
        scaled-grid (helper/scale-grid grid scale)
        tiled-map (module/place-module (tiled/tmx-tiled-map module/modules-file)
                                       scaled-grid
                                       grid
                                       (filter #(= :ground     (get grid %)) (g2d/posis grid))
                                       (filter #(= :transition (get grid %)) (g2d/posis grid)))
        start-position (mapv * start scale)
        can-spawn? #(= "all" (tiled/movement-property tiled-map %))
        _ (assert (can-spawn? start-position)) ; assuming hoping bottom left is movable
        spawn-positions (helper/flood-fill scaled-grid start-position can-spawn?)
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
        scaled-area-level-grid (helper/scale-grid area-level-grid scale)
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
    (creature-layer/add-creatures-layer! tiled-map creatures)
    {:tiled-map tiled-map
     :start-position (get-free-position-in-area-level 0)
     :area-level-grid scaled-area-level-grid}))

(defn create
  [{:keys [creature-properties
           graphics]
    :as params}]
  (generate-modules params
                    (creature-tiles/prepare creature-properties graphics)))
