(ns cdq.timer)

(declare elapsed-time)

(defn init! []
  (.bindRoot #'elapsed-time 0))

(defn inc-state! [delta-ms]
  (alter-var-root #'elapsed-time + delta-ms))

(defn create [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn reset [{:keys [duration] :as timer}]
  (assoc timer :stop-time (+ elapsed-time duration)))

(defn ratio [{:keys [duration stop-time] :as timer}]
  {:post [(<= 0 % 1)]}
  (if (stopped? timer)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))
