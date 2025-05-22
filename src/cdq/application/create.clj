(ns cdq.application.create
  (:require cdq.create.assets
            cdq.db
            [cdq.g :as g]
            [cdq.utils :refer [mapvals
                               io-slurp-edn
                               safe-get]]
            [gdl.graphics :as graphics]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]))

(defn- create-app-state []
  (let [config (let [m (io-slurp-edn "config.edn")]
                 (reify clojure.lang.ILookup
                   (valAt [_ k]
                     (safe-get m k))))
        batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (graphics/ui-viewport (:ui-viewport config))
        stage (ui/stage (:java-object ui-viewport)
                        (:java-object batch))]
    (run! require (:requires config))
    (ui/load! (:ui config))
    (input/set-processor! stage)
    (cdq.g/map->Game
     {:ctx/config config
      :ctx/db (cdq.db/create (:db config))
      :ctx/assets (cdq.create.assets/create (:assets config))
      :ctx/batch batch
      :ctx/unit-scale 1
      :ctx/world-unit-scale world-unit-scale
      :ctx/shape-drawer-texture shape-drawer-texture
      :ctx/shape-drawer (graphics/shape-drawer batch (graphics/texture-region shape-drawer-texture 1 0 1 1))
      :ctx/cursors (mapvals
                    (fn [[file [hotspot-x hotspot-y]]]
                      (graphics/cursor (format (:cursor-path-format config) file)
                                       hotspot-x
                                       hotspot-y))
                    (:cursors config))
      :ctx/default-font (graphics/truetype-font (:default-font config))
      :ctx/world-viewport (graphics/world-viewport world-unit-scale (:world-viewport config))
      :ctx/ui-viewport ui-viewport
      :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                         (tiled/renderer tiled-map
                                                         world-unit-scale
                                                         (:java-object batch))))
      :ctx/stage stage})))

(defn do! []
  (g/reset-game-state! (create-app-state)))
