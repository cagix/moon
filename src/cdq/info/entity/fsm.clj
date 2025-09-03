(ns cdq.info.entity.fsm)

(defn info-segment [[_ fsm] _ctx]
  (str "State: " (name (:state fsm))))
