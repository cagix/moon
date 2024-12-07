(ns forge.property)

(defn image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))

