(ns moon.properties.items
  (:require [moon.component :as component :refer [defc]]
            [moon.item :as item]
            [moon.modifiers :as mods]
            [moon.property :as property]))

(property/def :properties/items
  {:schema [:property/pretty-name
            :entity/image
            :item/slot
            [:item/modifiers {:optional true}]]
   :overview {:title "Items"
              :columns 20
              :image/scale 1.1
              :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                     (name slot)
                                     "")
                             (name (:property/id %)))}})

(defc :item/slot
  {:schema (apply vector :enum (keys item/empty-inventory))})

(defc :item/modifiers
  {:schema [:s/components-ns :modifier]}
  (component/info [[_ value-mods]]
    (str (mods/info-text value-mods)
         "\n [GRAY]"
         (binding [*print-level* nil]
           (with-out-str
            (clojure.pprint/pprint
             value-mods)))
         "[]")))
