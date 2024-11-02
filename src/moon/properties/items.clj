(ns moon.properties.items
  (:require [moon.component :as component]
            [moon.item :as item]
            [moon.modifiers :as mods]
            [moon.property :as property]))

(property/def :properties/items
  {:overview {:title "Items"
              :columns 20
              :image/scale 1.1
              :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                     (name slot)
                                     "")
                             (name (:property/id %)))}})

(defc :item/modifiers
  (component/info [[_ value-mods]]
    (str (mods/info-text value-mods)
         "\n [GRAY]"
         (binding [*print-level* nil]
           (with-out-str
            (clojure.pprint/pprint
             value-mods)))
         "[]")))
