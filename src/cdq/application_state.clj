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

; next step we have to somehow merge together used fields together as far as possible ?!
; grep ctx/ ...

(extend-type gdl.application.Context
  c/Graphics
  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/shape-drawer
                                    ctx/world-unit-scale]
                             :as ctx}
                            fns]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (doseq [f fns]
                                        (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale))

  (sprite [{:keys [ctx/world-unit-scale] :as ctx} texture-path]
    (graphics/sprite (graphics/texture-region (c/texture ctx texture-path))
                     world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
    (graphics/sprite (graphics/sub-region (:texture-region sprite) x y w h)
                     world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale] :as ctx} texture-path tilew tileh]
    {:image (graphics/sprite (graphics/texture-region (c/texture ctx texture-path))
                             world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [ctx {:keys [image tilew tileh]} [x y]]
    (c/sub-sprite ctx image [(* x tilew) (* y tileh) tilew tileh])))

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
