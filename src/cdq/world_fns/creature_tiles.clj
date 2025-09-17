(ns cdq.world-fns.creature-tiles
  (:require [cdq.graphics :as graphics]
            [clojure.utils :as utils]))

(defn prepare [creature-properties graphics]
  (for [creature creature-properties
        :let [image (first (:animation/frames (:entity/animation creature)))
              texture-region (graphics/texture-region graphics image)]]
    (utils/safe-merge creature
                      {:tile/id (:property/id creature)
                       :tile/texture-region texture-region})))
