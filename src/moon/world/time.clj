(ns moon.world.time)

(declare ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed)

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

(declare ^{:doc "The game logic update delta-time. Different then gdl.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         delta)

(defn pass [delta-ms]
  (alter-var-root #'elapsed + delta-ms)
  (bind-root #'delta delta-ms))

(defn init []
  (bind-root #'elapsed 0)
  (bind-root #'delta nil))
