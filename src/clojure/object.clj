(ns clojure.object)

(defn pipeline
  ([object functions]
   (reduce (fn [object f]
             (f object))
           object
           functions))

  ([{:keys [object functions]}]
   (pipeline object functions)))
