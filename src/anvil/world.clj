(ns anvil.world
  (:require [anvil.world.content-grid :as content-grid]
            [clojure.gdx.math.shapes :refer [rectangle->tiles
                                             circle->outer-rectangle
                                             rect-contains?
                                             overlaps?]]))

(declare ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed-time)

(defn timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn reset-timer [{:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed-time duration)))

(defn finished-ratio [{:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(declare ^{:doc "The game logic update delta-time. Different then forge.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         world-delta)

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

(declare tiled-map
         explored-tile-corners
         player-eid
         entity-ids)

(defn all-entities []
  (vals entity-ids))

(def mouseover-eid nil)

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

(declare content-grid)

(defn active-entities []
  (content-grid/active-entities content-grid
                                @player-eid))

(defprotocol Cell
  (cell-blocked? [cell* z-order])
  (blocks-vision? [cell*])
  (occupied-by-other? [cell* eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [cell* faction])
  (nearest-entity-distance [cell* faction]))

(declare grid)

(defn rectangle->cells [rectangle]
  (into [] (keep grid) (rectangle->tiles rectangle)))

(defn circle->cells [circle]
  (->> circle
       circle->outer-rectangle
       rectangle->cells))

(defn cells->entities [cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [circle]
  (->> (circle->cells circle)
       (map deref)
       cells->entities
       (filter #(overlaps? circle @%))))

(def ^:private offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]])

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(defn get-8-neighbour-positions [position]
  (mapv #(mapv + position %) offsets))

#_(defn- get-8-neighbour-positions [[x y]]
    (mapv (fn [tx ty]
            [tx ty])
          (range (dec x) (+ x 2))
          (range (dec y) (+ y 2))))

(defn cached-adjacent-cells [cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (into [] (keep grid) (-> @cell :position get-8-neighbour-positions))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn point->entities [position]
  (when-let [cell (get grid (mapv int position))]
    (filter #(rect-contains? @% position)
            (:entities @cell))))
