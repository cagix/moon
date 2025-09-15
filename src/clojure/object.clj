(ns clojure.object)

(defn pipeline
  ([pipeline*]
   (pipeline {} pipeline*))
  ([object pipeline]
   (reduce (fn [object [f & params]]
             (apply f object params))
           object
           pipeline)))
