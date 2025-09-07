(ns cdq.application)

(def state (atom nil))

(defn self-reference [ctx]
  (assoc ctx :ctx/application-state state))
