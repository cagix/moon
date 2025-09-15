(ns cdq.properties.creatures)

(def sort-by-fn #(vector (:creature/level %)
                         (name (:entity/species %))
                         (name (:property/id %))))

(def extra-info-text #(str (:creature/level %)))
