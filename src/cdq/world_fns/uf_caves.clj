(ns cdq.world-fns.uf-caves
  (:require cdq.world-fns.initial-grid-creation
            cdq.world-fns.fix-nads
            cdq.world-fns.create-star
            [clojure.graphics.texture :as texture]
            [com.badlogic.gdx.maps.tiled :as tiled]))

(defn create
  [{:keys [level/creature-properties
           textures
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
           :level/create-tile (let [texture (get textures texture-path)]
                                (memoize
                                 (fn [& {:keys [sprite-idx movement]}]
                                   {:pre [#{"all" "air" "none"} movement]}
                                   (tiled/static-tiled-map-tile
                                    (texture/region texture
                                                    (* (sprite-idx 0) tile-size)
                                                    (* (sprite-idx 1) tile-size)
                                                    tile-size
                                                    tile-size)
                                    "movement" movement))))
           :level/spawn-rate spawn-rate
           :level/scaling scaling
           :level/creature-properties creature-properties}
          [cdq.world-fns.initial-grid-creation/do!
           cdq.world-fns.fix-nads/do!
           cdq.world-fns.create-star/do!]))
