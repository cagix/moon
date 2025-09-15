(ns cdq.world-fns.uf-caves
  (:require [clojure.utils :as utils]
            [cdq.world-fns.creature-tiles :as creature-tiles]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.maps.tiled.tiles.static-tiled-map-tile :as static-tiled-map-tile]))

(defn create
  [{:keys [creature-properties
           graphics
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
           :level/create-tile (let [texture (utils/safe-get (:ctx/textures graphics) texture-path)]
                                (memoize
                                 (fn [& {:keys [sprite-idx movement]}]
                                   {:pre [#{"all" "air" "none"} movement]}
                                   (static-tiled-map-tile/create (texture/region texture
                                                                                 (* (sprite-idx 0) tile-size)
                                                                                 (* (sprite-idx 1) tile-size)
                                                                                 tile-size
                                                                                 tile-size)
                                                                 "movement" movement))))
           :level/spawn-rate spawn-rate
           :level/scaling scaling
           :level/creature-properties (creature-tiles/prepare creature-properties graphics)}
          (map requiring-resolve '[cdq.world-fns.initial-grid-creation/do!
                                   cdq.world-fns.fix-nads/do!
                                   cdq.world-fns.create-star/do!])))
