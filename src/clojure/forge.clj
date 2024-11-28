(in-ns 'clojure.core)

(defn mapvals [f m]
  (into {} (for [[k v] m]
             [k (f v)])))

(defn safe-get [m k]
  (let [result (get m k ::not-found)]
    (if (= result ::not-found)
      (throw (IllegalArgumentException. (str "Cannot find " (pr-str k))))
      result)))

(defn bind-root [avar value]
  (.bindRoot avar value))
