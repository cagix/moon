(ns clojure.object)

(defn pipeline
  ([opts]
   (pipeline (:object opts)
             (:pipeline opts)))
  ([object pipeline]
   (reduce (fn [object [f & params]]
             (apply f object params))
           object
           pipeline)))
