(ns cdq.time)

(defn create [context]
  (assoc context :gdl.context/elapsed-time 0))
