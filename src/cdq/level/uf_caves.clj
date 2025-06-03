(ns cdq.level.uf-caves
  (:require [gdl.gdx :as gdx]
            [gdl.graphics.texture :as texture]
            [cdq.grid2d :as g2d]
            [cdq.level.helper :refer [prepare-creature-properties
                                          add-creatures-layer!
                                          wgt-grid->tiled-map
                                          adjacent-wall-positions
                                          scalegrid
                                          flood-fill]]
            [cdq.rand :refer [get-rand-weighted-item]]
            [gdl.tiled :as tiled]))

(defn- rand-0-3 []
  (get-rand-weighted-item {0 60 1 1 2 1 3 1}))

(defn- rand-0-5 []
  (get-rand-weighted-item {0 30 1 1 2 1 3 1 4 1 5 1}))

(def ^:private tm-tile
  (memoize
   (fn [texture-region movement]
     {:pre [#{"all" "air" "none"} movement]}
     (gdx/static-tiled-map-tile texture-region "movement" movement))))

(def ^:private sprite-size 48)

(defn- uf-tile [texture & {:keys [sprite-x sprite-y movement]}]
  (tm-tile (texture/region texture
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

(defn- ground-tile [texture [x y]]
  (uf-tile texture
           :sprite-x (+ x (rand-0-3))
           :sprite-y y
           :movement "all"))

(defn- wall-tile [texture [x y]]
  (uf-tile texture
           :sprite-x (+ x (rand-0-5))
           :sprite-y y
           :movement "none"))

(defn- transition-tile [texture [x y]]
  (uf-tile texture
           :sprite-x (+ x (rand-0-5))
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

(defn- generate-tiled-map [texture grid]
  (let [ground-idx (rand-nth uf-grounds)
        {wall-x 0 wall-y 1 :as wall-idx} (rand-nth uf-walls)
        transition-idx [wall-x (inc wall-y)]
        position->tile (fn [position]
                         (case (get grid position)
                           :wall (wall-tile texture wall-idx)
                           :transition (if (transition? grid position)
                                         (transition-tile texture transition-idx)
                                         (wall-tile texture wall-idx))
                           :ground (ground-tile texture ground-idx)))]
    (wgt-grid->tiled-map sprite-size grid position->tile)))

; TODO don't spawn my faction vampire w. player items ...
; FIXME - overlapping with player - don't spawn creatures on start position
(defn- create* [{:keys [level/grid
                        level/start
                        level/spawn-rate
                        level/creature-properties
                        level/texture
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
        tiled-map (generate-tiled-map texture grid)
        ; -

        ; - calculate spawn positions -
        can-spawn? #(= "all" (tiled/movement-property tiled-map %))
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

(defn initial-grid-creation [{:keys [level/size
                                     level/cave-style
                                     level/random]
                              :as level}]
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

(def level-generator-steps [initial-grid-creation
                            fix-nads
                            create*])

; => TODO params for each step?

(defn create [{:keys [ctx/assets]
               :as ctx}]
  (reduce (fn [level f]
            (f level))
          ; TODO add uf-caves info
          ; and probabilities for each tile
          {:level/texture (assets "maps/uf_terrain.png")
           :level/random (java.util.Random.)
           :level/size 200
           :level/cave-style :wide
           :level/spawn-rate 0.02
           :level/scaling 3
           :level/creature-properties (prepare-creature-properties ctx)}
          level-generator-steps))
