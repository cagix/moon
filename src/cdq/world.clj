(ns cdq.world)

(defprotocol World
  (cell [_ position])
  (timer [_ duration])
  (stopped? [_ timer])
  (reset-timer [_ timer])
  (timer-ratio [_ timer]))
