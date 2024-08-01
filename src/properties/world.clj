(ns properties.world
  (:require [core.component :as component]
            [core.data :as data]
            [api.properties :as properties]))

(component/def :world/map-size data/pos-int-attr)
(component/def :world/max-area-level data/pos-int-attr) ; TODO <= map-size !?
(component/def :world/princess {:schema [:qualified-keyword {:namespace :creatures}]})
(component/def :world/spawn-rate data/pos-attr) ; TODO <1 !

(component/def :properties/world {}
  _
  (properties/create [_]
    {:id-namespace "worlds"
     :schema (data/map-attribute-schema
              [:property/id [:qualified-keyword {:namespace :worlds}]]
              [:world/map-size
               :world/max-area-level
               :world/princess
               :world/spawn-rate])
     :edn-file-sort-order 5
     :overview {:title "Worlds"
                :columns 10
                :image/dimensions [96 96]}}))