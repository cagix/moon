(ns cdq.time)

(defn create [context config]
  (assoc context :gdl.context/elapsed-time 0))
