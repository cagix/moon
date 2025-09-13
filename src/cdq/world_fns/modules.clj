(ns cdq.world-fns.modules
  (:require [cdq.grid2d :as g2d]
            [cdq.world-fns.helper :as helper]
            [cdq.world-fns.area-level-grid :as area-level-grid]
            [cdq.world-fns.module :as module]
            [cdq.world-fns.creature-tiles :as creature-tiles]
            [cdq.world-fns.creature-layer :as creature-layer]
            [cdq.world-fns.modules.place-modules]
            [clojure.gdx.maps.tiled :as tiled]))

(defn- generate-modules
  [{:keys [
           world/max-area-level
           world/spawn-rate
           creature-properties
           grid
           start
           scale
           scaled-grid
           tiled-map
           ]}]
  (let [
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

(defn- prepare-creature-properties
  [{:keys [creature-properties
           graphics]
    :as world-fn-ctx}]
  (update world-fn-ctx :creature-properties creature-tiles/prepare graphics))

(defn- assert-max-area-level
  [{:keys [world/map-size
           world/max-area-level]
    :as world-fn-ctx}]
  (assert (<= max-area-level map-size))
  world-fn-ctx)

(defn- create-initial-grid
  [{:keys [world/map-size]
    :as world-fn-ctx}]
  (let [{:keys [start grid]} (helper/cave-grid :size map-size)]
    (assoc world-fn-ctx :start start :grid grid)))

(defn- print-grid! [{:keys [grid] :as world-fn-ctx}]
  (helper/printgrid grid)
  (println " - ")
  world-fn-ctx)

(defn- assoc-transition-tiles
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

(defn create
  [world-fn-ctx]
  (-> world-fn-ctx
      (assoc :scale module/modules-scale)
      assert-max-area-level
      prepare-creature-properties
      create-initial-grid
      print-grid!
      assoc-transition-tiles
      print-grid!
      create-scaled-grid
      cdq.world-fns.modules.place-modules/do!
      generate-modules))
