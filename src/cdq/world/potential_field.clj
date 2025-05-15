(ns cdq.world.potential-field
  (:require [cdq.entity :as entity] ; just entity/faction
            [cdq.utils :refer [utils-positions when-seq]]
            [cdq.vector2 :as v]
            [cdq.world.grid :as grid :refer [rectangle->cells
                                             cached-adjacent-cells
                                             blocked?
                                             occupied-by-other?
                                             nearest-entity
                                             nearest-entity-distance
                                             get-8-neighbour-positions]]))

(let [order (get-8-neighbour-positions [0 0])]
  (def ^:private diagonal-check-indizes
    (into {} (for [[x y] (filter v/diagonal-direction? order)]
               [(first (utils-positions #(= % [x y]) order))
                (vec (utils-positions #(some #{%} [[x 0] [0 y]])
                                     order))]))))

(defn- is-not-allowed-diagonal? [at-idx adjacent-cells]
  (when-let [[a b] (get diagonal-check-indizes at-idx)]
    (and (nil? (adjacent-cells a))
         (nil? (adjacent-cells b)))))

(defn- remove-not-allowed-diagonals [adjacent-cells]
  (remove nil?
          (map-indexed
            (fn [idx cell]
              (when-not (or (nil? cell)
                            (is-not-allowed-diagonal? idx adjacent-cells))
                cell))
            adjacent-cells)))

; not using filter because nil cells considered @ remove-not-allowed-diagonals
; TODO only non-nil cells check
; TODO always called with cached-adjacent-cells ...
(defn- filter-viable-cells [eid adjacent-cells]
  (remove-not-allowed-diagonals
    (mapv #(when-not (or (grid/pf-cell-blocked? @%)
                         (occupied-by-other? @% eid))
             %)
          adjacent-cells)))

(defn- get-min-dist-cell [distance-to cells]
  (when-seq [cells (filter distance-to cells)]
    (apply min-key distance-to cells)))

; rarely called -> no performance bottleneck
(defn- viable-cell? [grid distance-to own-dist eid cell]
  (when-let [best-cell (get-min-dist-cell
                        distance-to
                        (filter-viable-cells eid (cached-adjacent-cells grid cell)))]
    (when (< (float (distance-to best-cell)) (float own-dist))
      cell)))

(defn- find-next-cell
  "returns {:target-entity eid} or {:target-cell cell}. Cell can be nil."
  [grid eid own-cell]
  (let [faction (entity/enemy @eid)
        distance-to    #(nearest-entity-distance @% faction)
        nearest-entity #(nearest-entity          @% faction)
        own-dist (distance-to own-cell)
        adjacent-cells (cached-adjacent-cells grid own-cell)]
    (if (and own-dist (zero? (float own-dist)))
      {:target-entity (nearest-entity own-cell)}
      (if-let [adjacent-cell (first (filter #(and (distance-to %)
                                                  (zero? (float (distance-to %))))
                                            adjacent-cells))]
        {:target-entity (nearest-entity adjacent-cell)}
        {:target-cell (let [cells (filter-viable-cells eid adjacent-cells)
                            min-key-cell (get-min-dist-cell distance-to cells)]
                        (cond
                         (not min-key-cell)  ; red
                         own-cell

                         (not own-dist)
                         min-key-cell

                         (> (float (distance-to min-key-cell)) (float own-dist)) ; red
                         own-cell

                         (< (float (distance-to min-key-cell)) (float own-dist)) ; green
                         min-key-cell

                         (= (distance-to min-key-cell) own-dist) ; yellow
                         (or
                          (some #(viable-cell? grid distance-to own-dist eid %) cells)
                          own-cell)))}))))

(defn- inside-cell? [grid entity cell]
  (let [cells (rectangle->cells grid entity)]
    (and (= 1 (count cells))
         (= cell (first cells)))))

; TODO work with entity !? occupied-by-other? works with entity not entity ... not with ids ... hmmm
(defn find-direction [grid eid] ; TODO pass faction here, one less dependency.
  (let [position (:position @eid)
        own-cell (grid (mapv int position))
        {:keys [target-entity target-cell]} (find-next-cell grid eid own-cell)]
    (cond
     target-entity
     (v/direction position (:position @target-entity))

     (nil? target-cell)
     nil

     :else
     (when-not (and (= target-cell own-cell)
                    (occupied-by-other? @own-cell eid)) ; prevent friction 2 move to center
       (when-not (inside-cell? grid @eid target-cell)
         (v/direction position (:middle @target-cell)))))))
