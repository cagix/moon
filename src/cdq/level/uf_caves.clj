(ns cdq.level.uf-caves
  (:require [cdq.grid2d :as g2d]
            [cdq.level.helper :refer [prepare-creature-properties
                                      add-creatures-layer!
                                      adjacent-wall-positions
                                      scalegrid
                                      flood-fill]]
            [cdq.rand :refer [get-rand-weighted-item]]
            [cdq.utils.tiled :as utils.tiled]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics :as graphics]
            [gdl.tiled :as tiled]))

(defn- assoc-transition-cells [grid]
  (let [grid (reduce #(assoc %1 %2 :transition) grid
                     (adjacent-wall-positions grid))]
    (assert (or
             (= #{:wall :ground :transition} (set (g2d/cells grid)))
             (= #{:ground :transition}       (set (g2d/cells grid))))
            (str "(set (g2d/cells grid)): " (set (g2d/cells grid))))
    ;_ (printgrid grid)
    ;_ (println)
    grid))

(defn- scale-grid [grid start scale]
  (let [grid (scalegrid grid scale)]
    ;_ (printgrid grid)
    ;_ (println)
    {:start-position (mapv #(* % scale) start)
     :grid grid}))

(defn- position->tile-fn [grid]
  (let [uf-grounds (for [x [1 5]
                         y (range 5 11)
                         :when (not= [x y] [5 5])] ; wooden
                     [x y])
        uf-walls (for [x [1]
                       y [13,16,19,22,25,28]]
                   [x y])
        transition? (fn [[x y]]
                      (= :ground (get grid [x (dec y)])))
        rand-0-3 (fn [] (get-rand-weighted-item {0 60 1 1 2 1 3 1}))
        rand-0-5 (fn [] (get-rand-weighted-item {0 30 1 1 2 1 3 1 4 1 5 1}))
        [ground-x ground-y] (rand-nth uf-grounds)
        {wall-x 0 wall-y 1} (rand-nth uf-walls)
        [transition-x transition-y] [wall-x (inc wall-y)]
        wall-tile (fn []
                    {:sprite-idx [(+ wall-x (rand-0-5)) wall-y]
                     :movement "none"})
        transition-tile (fn []
                          {:sprite-idx [(+ transition-x (rand-0-5))
                                        transition-y]
                           :movement "none"})
        ground-tile (fn []
                      {:sprite-idx [(+ ground-x (rand-0-3))
                                    ground-y]
                       :movement "all"})]
    (fn [position]
      (case (get grid position)
        :wall (wall-tile)
        :transition (if (transition? position)
                      (transition-tile)
                      (wall-tile))
        :ground (ground-tile)))))

; TODO don't spawn my faction vampire w. player items ...
; FIXME - overlapping with player - don't spawn creatures on start position
(defn- create* [{:keys [level/grid
                        level/start
                        level/spawn-rate
                        level/creature-properties
                        level/create-tile
                        level/tile-size
                        level/scaling]}]
  (assert (= #{:wall :ground} (set (g2d/cells grid))))
  (let [

        ; - next step scaling -
        {:keys [start-position grid]} (scale-grid grid start scaling)
        ; -

        ; - next step transition cells -
        grid (assoc-transition-cells grid)
        ; -

        ; - create tiled-map - (could do this at the end .... check spawn positions from grid itself ?)
        position->tile (position->tile-fn grid)
        tiled-map (tiled/create-tiled-map
                   {:properties {"width"  (g2d/width  grid)
                                 "height" (g2d/height grid)
                                 "tilewidth"  tile-size
                                 "tileheight" tile-size}
                    :layers [{:name "ground"
                              :visible? true
                              :properties {"movement-properties" true}
                              :tiles (for [position (g2d/posis grid)]
                                       [position (create-tile (position->tile position))])}]})
        ; -

        ; - calculate spawn positions -
        can-spawn? #(= "all" (utils.tiled/movement-property tiled-map %))
        _ (assert (can-spawn? start-position)) ; assuming hoping bottom left is movable
        level (inc (rand-int 6)) ;;; oooh fuck we have a level ! -> go through your app remove all hardcoded values !!!! secrets lie in the shadows ! functional programming FTW !
        creatures (filter #(= level (:creature/level %)) creature-properties)
        spawn-positions (flood-fill grid start-position can-spawn?)
        creatures (for [position spawn-positions
                        :when (<= (rand) spawn-rate)]
                    [position (rand-nth creatures)])]
    ; - add creature layer -
    (add-creatures-layer! tiled-map creatures)
    ; - finished -
    {:tiled-map tiled-map
     :start-position start-position}))

(require '[cdq.level.caves :as caves])

(defn initial-grid-creation [level
                             {:keys [size
                                     cave-style
                                     random]}]
  (let [{:keys [start grid]} (caves/create random size size cave-style)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    (assoc level
           :level/start start
           :level/grid grid)))

(require '[cdq.level.nads :as nads])

(defn fix-nads [{:keys [level/grid]
                 :as level}]
  (assert (= #{:wall :ground} (set (g2d/cells grid))))
  (let [grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    (assoc level :level/grid grid)))

(defn create [{:keys [ctx/graphics]
               :as ctx}]
  (let [tile-size 48]
    (reduce (fn [level step]
              (if (vector? step)
                (let [[f params] step] (f level params))
                (let [f step]          (f level))))
            ; TODO add uf-caves info
            ; and probabilities for each tile
            {:level/tile-size tile-size
             :level/create-tile (let [texture (graphics/texture graphics "maps/uf_terrain.png")]
                                  (memoize
                                   (fn [& {:keys [sprite-idx movement]}]
                                     {:pre [#{"all" "air" "none"} movement]}
                                     (tiled/static-tiled-map-tile (texture-region/create texture
                                                                                         (* (sprite-idx 0) tile-size)
                                                                                         (* (sprite-idx 1) tile-size)
                                                                                         tile-size
                                                                                         tile-size)
                                                                  "movement" movement))))
             :level/spawn-rate 0.02
             :level/scaling 3
             :level/creature-properties (prepare-creature-properties ctx)}
            [[initial-grid-creation {:size 200
                                     :cave-style :wide
                                     :random (java.util.Random.)}]
             fix-nads
             create*])))
