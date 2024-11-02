(ns moon.properties.worlds
  (:require [moon.property :as property]))

#_(defc :world/max-area-level {:schema pos-int?}) ; TODO <= map-size !?
#_(defc :world/spawn-rate {:schema pos?}) ; TODO <1 !

(property/def :properties/worlds
  {:overview {:title "Worlds"
              :columns 10}})
