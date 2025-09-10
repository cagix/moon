(ns cdq.world-fns.creature-tiles
  (:require [cdq.property :as property]
            [cdq.gdx.graphics :as graphics]
            [cdq.utils :as utils]))

(defn prepare [creature-properties graphics]
  (for [creature creature-properties
        :let [texture-region (graphics/texture-region graphics (property/image creature))]]
    (utils/safe-merge creature
                      {:tile/id (:property/id creature)
                       :tile/texture-region texture-region})))
