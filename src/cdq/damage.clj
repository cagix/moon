(ns cdq.damage)

(defn info-text [{[min max] :damage/min-max}]
  (str min "-" max " damage"))
