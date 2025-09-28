(ns cdq.world-fns.modules
  (:require [cdq.world-fns.area-level-grid :as area-level-grid]
            [cdq.world-fns.creature-layer :as creature-layer]
            cdq.world-fns.modules.place-modules
            [gdl.grid2d :as g2d]
            [gdl.grid2d.caves :as caves]
            [gdl.grid2d.nads :as nads]
            [gdl.grid2d.utils :as helper]
            [gdl.maps.tiled :as tiled]))

(defn- add-scale [w]
  (assoc w :scale [32 20]))

(defn- assert-max-area-level
  [{:keys [world/map-size
           world/max-area-level]
    :as world-fn-ctx}]
  (assert (<= max-area-level map-size))
  world-fn-ctx)

(defn- cave-grid [& {:keys [size]}]
  (let [{:keys [start grid]} (caves/create (java.util.Random.) size size :wide)
        grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    {:start start
     :grid grid}))

(defn- create-initial-grid
  [{:keys [world/map-size]
    :as world-fn-ctx}]
  (let [{:keys [start grid]} (cave-grid :size map-size)]
    (assoc world-fn-ctx
           :start start
           :grid grid)))

(defn- print-grid! [{:keys [grid] :as world-fn-ctx}]
  (helper/printgrid grid)
  (println " - ")
  world-fn-ctx)

(defn- assoc-transitions
  [{:keys [grid] :as world-fn-ctx}]
  (let [grid (reduce #(assoc %1 %2 :transition)
                     grid
                     (helper/adjacent-wall-positions grid))]
    (assert (or
             (= #{:wall :ground :transition} (set (g2d/cells grid)))
             (= #{:ground :transition} (set (g2d/cells grid))))
            (str "(set (g2d/cells grid)): " (set (g2d/cells grid))))
    (assoc world-fn-ctx :grid grid)))

(defn- create-scaled-grid [w]
  (assoc w :scaled-grid (helper/scale-grid (:grid w) (:scale w))))

(defn- load-schema-tiled-map [w]
  (assoc w :schema-tiled-map (tiled/tmx-tiled-map "maps/modules.tmx")))

(defn- grid->tiled-map
  "Creates an empty new tiled-map with same layers and properties as schema-tiled-map.
  The size of the map is as of the grid, which contains also the tile information from the schema-tiled-map."
  [schema-tiled-map grid]
  (tiled/create-tiled-map
   {:properties (merge (tiled/map-properties schema-tiled-map)
                       {"width" (g2d/width grid)
                        "height" (g2d/height grid)})
    :layers (for [layer (tiled/layers schema-tiled-map)]
              {:name (tiled/layer-name layer)
               :visible? (tiled/visible? layer)
               :properties (tiled/map-properties layer)
               :tiles (for [position (g2d/posis grid)
                            :let [local-position (get grid position)]
                            :when local-position]
                        (when (vector? local-position)
                          (when-let [tile (tiled/tile-at layer local-position)]
                            [position (tiled/copy-tile tile)])))})}))

(defn- convert-to-tiled-map
  [{:keys [scaled-grid
           schema-tiled-map]
    :as w}]
  (assoc w :tiled-map (grid->tiled-map schema-tiled-map scaled-grid)))

(defn- calculate-start-position [{:keys [start scale] :as w}]
  (assoc w :start-position (mapv * start scale)))

(defn- finish-and-place-creatures!
  [{:keys [
           world/max-area-level
           world/spawn-rate
           level/creature-properties
           grid
           start
           scale
           scaled-grid
           tiled-map
           start-position
           ]}]
  (let [


        can-spawn? #(= "all" (tiled/movement-property tiled-map %))

        _ (assert (can-spawn? start-position)) ; assuming hoping bottom left is movable

        spawn-positions (helper/flood-fill scaled-grid start-position can-spawn?)
        ;_ (println "scaled grid with filled nil: '?' \n")
        ;_ (printgrid (reduce #(assoc %1 %2 nil) scaled-grid spawn-positions))
        ;_ (println "\n")

        {:keys [_steps area-level-grid]} (area-level-grid/create
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
  [world-fn-ctx]
  (-> world-fn-ctx
      add-scale
      assert-max-area-level
      create-initial-grid
      #_print-grid!
      assoc-transitions
      #_print-grid!
      create-scaled-grid
      load-schema-tiled-map
      cdq.world-fns.modules.place-modules/do!
      convert-to-tiled-map
      calculate-start-position
      finish-and-place-creatures!))
