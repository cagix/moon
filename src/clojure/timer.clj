(ns clojure.timer)

(defn create [elapsed-time duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]} elapsed-time]
  (>= elapsed-time stop-time))

(defn reset [{:keys [duration] :as timer} elapsed-time]
  (assoc timer :stop-time (+ elapsed-time duration)))

(defn ratio [{:keys [duration stop-time] :as timer} elapsed-time]
  {:post [(<= 0 % 1)]}
  (if (stopped? timer elapsed-time)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))
