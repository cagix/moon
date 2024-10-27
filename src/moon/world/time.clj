(ns moon.world.time)

(def elapsed "The elapsed in-game-time in seconds (not counting when game is paused).")

(defn timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed stop-time))

(defn reset [{:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed duration)))

(defn finished-ratio [{:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed) duration))))
