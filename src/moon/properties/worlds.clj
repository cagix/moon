(ns moon.properties.worlds
  (:require [moon.component :refer [defc]]
            [moon.property :as property]))

(defc :world/player-creature {:schema :some #_[:s/one-to-one :properties/creatures]})
(defc :world/map-size {:schema pos-int?})
(defc :world/max-area-level {:schema pos-int?}) ; TODO <= map-size !?
(defc :world/spawn-rate {:schema pos?}) ; TODO <1 !
(defc :world/tiled-map {:schema :string})
(defc :world/components {:schema [:s/map []]})
(defc :world/generator {:schema [:enum
                                 :world.generator/tiled-map
                                 :world.generator/modules
                                 :world.generator/uf-caves]})

(property/def :properties/worlds
  {:schema [:world/generator
            :world/player-creature
            [:world/tiled-map {:optional true}]
            [:world/map-size {:optional true}]
            [:world/max-area-level {:optional true}]
            [:world/spawn-rate {:optional true}]]
   :overview {:title "Worlds"
              :columns 10}})
