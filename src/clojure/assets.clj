(ns clojure.assets)

(defn sound [assets sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))
