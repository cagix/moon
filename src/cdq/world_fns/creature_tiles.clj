(ns cdq.world-fns.creature-tiles
  (:require [cdq.property :as property]
            [cdq.image :as image]
            [cdq.utils :as utils]))

(defn prepare [creature-properties textures]
  (for [creature creature-properties
        :let [texture-region (image/texture-region (property/image creature) textures)]]
    (utils/safe-merge creature
                      {:tile/id (:property/id creature)
                       :tile/texture-region texture-region})))
