(ns cdq.properties.items)

(def sort-by-fn #(vector (name (:item/slot %))
                         (name (:property/id %))))

(def extra-info-text (constantly ""))
