(ns clojure.object)

(defn pipeline [{:keys [object pipeline]}]
  (reduce (fn [object [f & params]]
            (apply f object params))
          object
          pipeline))
