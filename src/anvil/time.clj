(ns anvil.time)

(declare ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed)

(defn timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed stop-time))

(defn reset-timer [{:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed duration)))

(defn finished-ratio [{:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed) duration))))

(declare ^{:doc "The game logic update delta-time in ms."} delta)

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

(declare paused?)
