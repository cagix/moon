(ns gdl.utils)

(defn safe-get [m k]
  (let [result (get m k ::not-found)]
    (if (= result ::not-found)
      (throw (IllegalArgumentException. (str "Cannot find " (pr-str k))))
      result)))

(defn mapvals [f m]
  (into {} (for [[k v] m]
             [k (f v)])))
