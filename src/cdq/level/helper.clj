(ns cdq.level.helper
  (:require [cdq.db :as db]
            [cdq.grid2d :as g2d]
            [cdq.level.caves :as caves]
            [cdq.level.nads :as nads]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [clojure.gdx.maps.tiled :as tiled]
            [cdq.graphics :as graphics]))

(defn prepare-creature-properties [{:keys [ctx/graphics
                                           ctx/db]}]
  (for [creature (db/all-raw db :properties/creatures)
        :let [texture-region (graphics/image->texture-region graphics (property/image creature))]]
    (utils/safe-merge creature
                      {:tile/id (:property/id creature)
                       :tile/texture-region texture-region})))

; out of memory error -> each texture region is a new object
; so either memoize on id or property/image already calculated !? idk
(def creature-tile
  (memoize
   (fn [{:keys [tile/id
                tile/texture-region]}]
     (assert (and id
                  texture-region))
     (tiled/static-tiled-map-tile texture-region "id" id))))

(defn add-creatures-layer! [tiled-map spawn-positions]
  (tiled/add-layer! tiled-map {:name "creatures"
                               :visible? false
                               :tiles (for [[position creature-property] spawn-positions]
                                        [position (creature-tile creature-property)])}))

(defn scale-grid [grid [w h]]
  (g2d/create-grid (* (g2d/width grid)  w)
                   (* (g2d/height grid) h)
                   (fn [[x y]]
                     (get grid
                          [(int (/ x w))
                           (int (/ y h))]))))

(defn scalegrid [grid factor]
  (g2d/create-grid (* (g2d/width grid) factor)
                   (* (g2d/height grid) factor)
                   (fn [posi]
                     (get grid (mapv #(int (/ % factor)) posi)))))

(defn- print-cell [celltype]
  (print (if (number? celltype)
           celltype
           (case celltype
             nil               "?"
             :undefined        " "
             :ground           "_"
             :wall             "#"
             :airwalkable      "."
             :module-placement "X"
             :start-module     "@"
             :transition       "+"))))

(defn printgrid ; print-grid in data.grid2d is y-down
  "Prints with y-up coordinates."
  [grid]
  (doseq [y (range (dec (g2d/height grid)) -1 -1)]
    (doseq [x (range (g2d/width grid))]
      (print-cell (grid [x y])))
    (println)))

(let [idxvalues-order [[1 0] [-1 0] [0 1] [0 -1]]]
  (assert (= (g2d/get-4-neighbour-positions [0 0])
             idxvalues-order)))

(comment
  ; Values for every neighbour:
  {          [0 1] 1
   [-1 0]  8          [1 0] 2
             [0 -1] 4 })

; so the idxvalues-order corresponds to the following values for a neighbour tile:
(def ^:private idxvalues [2 8 1 4])

(defn- calculate-index-value [position->transition? idx position]
  (if (position->transition? position)
    (idxvalues idx)
    0))

(defn transition-idx-value [position position->transition?]
  (->> position
       g2d/get-4-neighbour-positions
       (map-indexed (partial calculate-index-value
                             position->transition?))
       (apply +)))

; TODO generates 51,52. not max 50
(defn cave-grid [& {:keys [size]}]
  (let [{:keys [start grid]} (caves/create (java.util.Random.) size size :wide)
        grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    {:start start
     :grid grid}))

(defn adjacent-wall-positions [grid]
  (filter (fn [p] (and (= :wall (get grid p))
                       (some #(= :ground (get grid %))
                             (g2d/get-8-neighbour-positions p))))
          (g2d/posis grid)))

(defn flood-fill [grid start walk-on-position?]
  (loop [next-positions [start]
         filled []
         grid grid]
    (if (seq next-positions)
      (recur (filter #(and (get grid %)
                           (walk-on-position? %))
                     (distinct
                      (mapcat g2d/get-8-neighbour-positions
                              next-positions)))
             (concat filled next-positions)
             (utils/assoc-ks grid next-positions nil))
      filled)))

(comment
 (let [{:keys [start grid]} (cave-grid :size 15)
       _ (println "BASE GRID:\n")
       _ (printgrid grid)
       ;_ (println)
       ;_ (println "WITH START POSITION (0) :\n")
       ;_ (printgrid (assoc grid start 0))
       ;_ (println "\nwidth:  " (g2d/width  grid)
       ;           "height: " (g2d/height grid)
       ;           "start " start "\n")
       ;_ (println (g2d/posis grid))
       _ (println "\n\n")
       filled (flood-fill grid start (fn [p] (= :ground (get grid p))))
       _ (printgrid (reduce #(assoc %1 %2 nil) grid filled))])
 )

(defn grid->tiled-map
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
