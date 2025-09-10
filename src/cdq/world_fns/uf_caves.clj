(ns cdq.world-fns.uf-caves
  (:require [cdq.utils :as utils]
            [cdq.world-fns.creature-tiles :as creature-tiles]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.maps.tiled :as tiled]))

(defn create
  [{:keys [creature-properties
           ctx
           tile-size
           texture-path
           spawn-rate
           scaling
           cave-size
           cave-style]}]
  (reduce (fn [m f]
            (f m))
          {:size cave-size
           :cave-style cave-style
           :random (java.util.Random.)
           :level/tile-size tile-size
           :level/create-tile (let [texture (utils/safe-get (:ctx/textures ctx) texture-path)]
                                (memoize
                                 (fn [& {:keys [sprite-idx movement]}]
                                   {:pre [#{"all" "air" "none"} movement]}
                                   (tiled/static-tiled-map-tile (texture/region texture
                                                                                (* (sprite-idx 0) tile-size)
                                                                                (* (sprite-idx 1) tile-size)
                                                                                tile-size
                                                                                tile-size)
                                                                "movement" movement))))
           :level/spawn-rate spawn-rate
           :level/scaling scaling
           :level/creature-properties (creature-tiles/prepare creature-properties ctx)}
          (map requiring-resolve '[cdq.world-fns.initial-grid-creation/do!
                                   cdq.world-fns.fix-nads/do!
                                   cdq.world-fns.create-star/do!])))
