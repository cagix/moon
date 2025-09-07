(ns cdq.application.context
  (:require [cdq.application.context.record :as ctx-record]
            [cdq.ctx :as ctx]
            cdq.gdx-app.dispose
            cdq.gdx-app.resize
            [cdq.malli :as m]
            [clojure.gdx.scenes.scene2d :as scene2d]))

(extend-type cdq.application.context.record.Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (ctx/handle-draws! ctx draws)))

(defn create []
  (reduce (fn [ctx f]
            (let [result (if (vector? f)
                           (let [[f params] f]
                             ((requiring-resolve f) ctx params))
                           ((requiring-resolve f) ctx))]
              (if (nil? result)
                ctx
                result)))
          (ctx-record/map->Context {:schema (m/schema ctx-record/schema)})
          '[cdq.create.txs/do!
            cdq.create.effects/do!
            cdq.create.editor-widgets/do!
            cdq.create.ui-actors/do!
            cdq.create.draw-on-world-viewport/do!
            cdq.create.config/do!
            cdq.create.vis-ui/do!
            cdq.create.colors/do!
            cdq.create.draw-fns/do!
            cdq.create.render-layers/do!
            cdq.create.info/do!
            cdq.create.sprite-batch/do!
            cdq.create.graphics/do!
            cdq.create.textures/do!
            cdq.create.audio/do!
            cdq.create.cursors/do!
            cdq.create.db/do!
            cdq.create.world-unit-scale/do!
            cdq.create.ui-viewport/do!
            cdq.create.input/do!
            cdq.create.stage/do!
            cdq.create.input-processor/do!
            cdq.create.tiled-map-renderer/do!
            cdq.create.world-viewport/do!
            cdq.create.default-font/do!
            cdq.create.unit-scale/do!
            cdq.create.shape-drawer-texture/do!
            cdq.create.shape-drawer/do!
            cdq.reset-game-state/do!
            cdq.create.frame/do!]))

(def dispose cdq.gdx-app.dispose/do!)

(defn render [ctx]
  (reduce (fn [ctx f]
            (if-let [new-ctx ((requiring-resolve f) ctx)]
              new-ctx
              ctx))
          ctx
          '[cdq.render.validate/do!
            cdq.render.assoc-mouseover-keys/do!
            cdq.render.update-mouseover-eid/do!
            cdq.render.check-open-debug-data/do! ; TODO FIXME its not documented I forgot rightclick can open debug data view!
            cdq.render.assoc-active-entities/do!
            cdq.render.set-camera-on-player/do!
            cdq.render.clear-screen/do!
            cdq.render.draw-world-map/do!
            cdq.render.draw-on-world-viewport/do!
            cdq.render.render-stage/do!
            cdq.render.set-cursor/do!
            cdq.render.player-state-handle-input/do!
            cdq.render.assoc-paused/do!
            cdq.render.tick-world/do!
            cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
            cdq.render.handle-key-input/do!
            cdq.render.dissoc-mouseover-keys/do!
            cdq.render.validate/do!
            ; :cdq.render/validate
            ; -> render multifn / method map ?
            ; tx just  method map ?
            ]))

(def resize cdq.gdx-app.resize/do!)
