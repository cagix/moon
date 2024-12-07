(ns ^:no-doc forge.mapgen.uf-caves
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.tiled :as tiled]
            [clojure.rand :refer [get-rand-weighted-item]]
            [data.grid2d :as g2d]
            [forge.app.asset-manager :refer [asset-manager]]
            [forge.app.db :as db]
            [forge.level :refer [generate-level*]]
            [forge.mapgen :refer [creatures-with-level creature-tile wgt-grid->tiled-map adjacent-wall-positions scalegrid cave-grid flood-fill]]))

(def ^:private scaling 4)

(defn- rand-0-3 []
  (get-rand-weighted-item {0 60 1 1 2 1 3 1}))

(defn- rand-0-5 []
  (get-rand-weighted-item {0 30 1 1 2 1 3 1 4 1 5 1}))

(defn- set-creatures-tiles [spawn-rate tiled-map spawn-positions]
  (let [layer (tiled/add-layer! tiled-map :name "creatures" :visible false)
        creatures (db/build-all :properties/creatures)
        level (inc (rand-int 6))
        creatures (creatures-with-level creatures level)]
    (doseq [position spawn-positions
            :when (<= (rand) spawn-rate)]
      (tiled/set-tile! layer position (creature-tile (rand-nth creatures))))))

(def ^:private tm-tile
  (memoize
   (fn [texture-region movement]
     {:pre [#{"all" "air" "none"} movement]}
     (let [tile (tiled/static-tiled-map-tile texture-region)]
       (tiled/put! (tiled/m-props tile) "movement" movement)
       tile))))

(def ^:private sprite-size 48)

(defn- uf-tile [& {:keys [sprite-x sprite-y movement]}]
  (tm-tile (g/texture-region (asset-manager "maps/uf_terrain.png")
                             (* sprite-x sprite-size)
                             (* sprite-y sprite-size)
                             sprite-size
                             sprite-size)
           movement))

(def ^:private uf-grounds
  (for [x [1 5]
        y (range 5 11)
        :when (not= [x y] [5 5])] ; wooden
    [x y]))

(def ^:private uf-walls
  (for [x [1]
        y [13,16,19,22,25,28]]
    [x y]))

(defn- ground-tile [[x y]]
  (uf-tile :sprite-x (+ x (rand-0-3))
           :sprite-y y
           :movement "all"))

(defn- wall-tile [[x y]]
  (uf-tile :sprite-x (+ x (rand-0-5))
           :sprite-y y
           :movement "none"))

(defn- transition-tile [[x y]]
  (uf-tile :sprite-x (+ x (rand-0-5))
           :sprite-y y
           :movement "none"))

(defn- transition? [grid [x y]]
  (= :ground (get grid [x (dec y)])))

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

(defn- generate-tiled-map [grid]
  (let [ground-idx (rand-nth uf-grounds)
        {wall-x 0 wall-y 1 :as wall-idx} (rand-nth uf-walls)
        transition-idx [wall-x (inc wall-y)]
        position->tile (fn [position]
                         (case (get grid position)
                           :wall (wall-tile wall-idx)
                           :transition (if (transition? grid position)
                                         (transition-tile transition-idx)
                                         (wall-tile wall-idx))
                           :ground (ground-tile ground-idx)))]
    (wgt-grid->tiled-map sprite-size grid position->tile)))

; TODO don't spawn my faction vampire w. player items ...
; FIXME - overlapping with player - don't spawn creatures on start position
(defn- create [{:keys [world/map-size world/spawn-rate]}]
  (let [{:keys [start grid]} (cave-grid :size map-size)
        {:keys [start-position grid]} (scale-grid grid start scaling)
        grid (assoc-transition-cells grid)
        tiled-map (generate-tiled-map grid)
        can-spawn? #(= "all" (tiled/movement-property tiled-map %))
        _ (assert (can-spawn? start-position)) ; assuming hoping bottom left is movable
        spawn-positions (flood-fill grid start-position can-spawn?)]
    (set-creatures-tiles spawn-rate tiled-map spawn-positions)
    {:tiled-map tiled-map
     :start-position start-position}))

(defmethod generate-level* :world.generator/uf-caves [world]
  (create world))

(defmethod generate-level* :world.generator/tiled-map [world]
  {:tiled-map (tiled/load-tmx-map (:world/tiled-map world))
   :start-position [32 71]})
