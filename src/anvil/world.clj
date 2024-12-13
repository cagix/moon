(ns anvil.world
  (:require [gdl.math.raycaster :as raycaster]))

(declare tiled-map

         explored-tile-corners

         grid

         entity-ids

         content-grid

         player-eid

         raycaster

         ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed-time

         ^{:doc "The game logic update delta-time in ms."}
         delta-time

         paused?

         mouseover-eid

         error)

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

(defn ray-blocked? [start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))

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

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))
