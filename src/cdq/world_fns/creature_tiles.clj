(ns cdq.world-fns.creature-tiles
  (:require [cdq.property :as property]
            [cdq.gdx.graphics :as graphics]
            [cdq.utils :as utils]))

(defn prepare [creature-properties ctx]
  (for [creature creature-properties
        :let [texture-region (graphics/texture-region ctx (property/image creature))]]
    (utils/safe-merge creature
                      {:tile/id (:property/id creature)
                       :tile/texture-region texture-region})))
