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

(defn- create-state! [config]
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))]
    (gdl.application/map->Context
     {:batch batch
      :unit-scale 1
      :world-unit-scale world-unit-scale
      :shape-drawer-texture shape-drawer-texture
      :shape-drawer (sd/create (:java-object batch)
                               (graphics/texture-region shape-drawer-texture 1 0 1 1))
      :cursors (utils/mapvals
                (fn [[file [hotspot-x hotspot-y]]]
                  (graphics/create-cursor (format (:cursor-path-format config) file)
                                          hotspot-x
                                          hotspot-y))
                (:cursors config))
      :default-font (graphics/truetype-font (:default-font config))})))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (create-state! config)
      (cdq.create.gdx/add-gdx!)
      (cdq.create.ui-viewport/add config)
      (cdq.create.stage/add-stage!)
      (cdq.create.assets/add-assets config)
      (cdq.create.config/add-config config)
      (cdq.create.db/add-db config)
      cdq.create.tiled-map-renderer/add
      (cdq.create.world-viewport/add config)
      ((requiring-resolve 'cdq.game-state/create!))))
