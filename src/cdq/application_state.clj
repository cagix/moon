(ns cdq.application-state
  (:require [cdq.create.assets]
            [cdq.create.config]
            [cdq.create.db]
            [cdq.create.gdx]
            [cdq.create.stage]
            [cdq.create.tiled-map-renderer]
            [cdq.create.ui-viewport]
            [cdq.create.world-viewport]
            [clojure.gdx.interop :as interop]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.application]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui]
            [gdl.utils :as utils]))

(defn- create-state! [ctx config]
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))]
    (merge ctx
           {:ctx/batch batch
            :ctx/unit-scale 1
            :ctx/world-unit-scale world-unit-scale
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create (:java-object batch)
                                         (graphics/texture-region shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals
                          (fn [[file [hotspot-x hotspot-y]]]
                            (graphics/create-cursor (format (:cursor-path-format config) file)
                                                    hotspot-x
                                                    hotspot-y))
                          (:cursors config))
            :ctx/default-font (graphics/truetype-font (:default-font config))})))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (create-state! config)
      (cdq.create.gdx/add-gdx!)
      (cdq.create.ui-viewport/add config)
      (cdq.create.stage/add-stage!)
      (cdq.create.assets/add-assets config)
      (cdq.create.config/add-config config)
      (cdq.create.db/add-db config)
      cdq.create.tiled-map-renderer/add
      (cdq.create.world-viewport/add config)
      ((requiring-resolve 'cdq.game-state/create!))))
