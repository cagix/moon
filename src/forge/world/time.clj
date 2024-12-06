(ns forge.world.time
  (:require [clojure.gdx.graphics :as g]
            [forge.utils :refer [bind-root]]))

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

(defn init [_]
  (bind-root elapsed-time 0)
  (bind-root world-delta nil))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

(defn frame-tick []
  (let [delta-ms (min (g/delta-time) max-delta-time)]
    (alter-var-root #'elapsed-time + delta-ms)
    (bind-root world-delta delta-ms)))
