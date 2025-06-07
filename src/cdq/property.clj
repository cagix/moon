(ns cdq.property
  (:refer-clojure :exclude [type]))

(defn type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn type->id-namespace [property-type]
  (keyword (name property-type)))

(defn image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))
