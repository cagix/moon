(ns cdq.application.render
  (:require cdq.render.validate
            cdq.render.update-mouse
            cdq.render.update-mouseover-eid
            cdq.render.check-open-debug
            cdq.render.assoc-active-entities
            cdq.render.set-camera-on-player
            cdq.render.clear-screen
            cdq.render.draw-world-map
            cdq.render.draw-on-world-viewport
            cdq.render.draw-on-world-viewport.tile-grid
            cdq.render.draw-on-world-viewport.cell-debug
            cdq.render.draw-on-world-viewport.entities
            #_cdq.render.draw-on-world-viewport.geom-test
            cdq.render.draw-on-world-viewport.highlight-mouseover-tile
            cdq.render.assoc-interaction-state
            cdq.render.set-cursor
            cdq.render.player-state-handle-input
            cdq.render.assoc-paused
            cdq.render.update-time
            cdq.render.update-potential-fields
            cdq.render.tick-entities
            cdq.render.remove-destroyed-entities
            cdq.render.handle-key-input
            [clojure.scene2d.stage :as stage]
            [clojure.utils :as utils]))

(def ^:private pipeline
  [[(fn [ctx]
      (if-let [new-ctx (stage/get-ctx (:ctx/stage ctx))]
        new-ctx
        ctx ; first render stage doesnt have context
        ))]
   [cdq.render.validate/do!]
   [cdq.render.update-mouse/do!]
   [cdq.render.update-mouseover-eid/do!]
   [cdq.render.check-open-debug/do!]
   [cdq.render.assoc-active-entities/do!]
   [cdq.render.set-camera-on-player/do!]
   [cdq.render.clear-screen/do!]
   [cdq.render.draw-world-map/do!]
   [cdq.render.draw-on-world-viewport/do! [
                                           [cdq.render.draw-on-world-viewport.tile-grid/do!]
                                           [cdq.render.draw-on-world-viewport.cell-debug/do!]
                                           [cdq.render.draw-on-world-viewport.entities/do!]
                                           #_ [cdq.render.draw-on-world-viewport.geom-test/do!]
                                           [cdq.render.draw-on-world-viewport.highlight-mouseover-tile/do!]
                                           ]]
   [cdq.render.assoc-interaction-state/do!]
   [cdq.render.set-cursor/do!]
   [cdq.render.player-state-handle-input/do!]
   [cdq.render.assoc-paused/do!]
   [cdq.render.update-time/do!]
   [cdq.render.update-potential-fields/do!]
   [cdq.render.tick-entities/do!]
   [cdq.render.remove-destroyed-entities/do!]
   [cdq.render.handle-key-input/do!]
   [(fn [{:keys [ctx/stage]
          :as ctx}]
      (stage/set-ctx! stage ctx)
      (stage/act!     stage)
      (stage/draw!    stage)
      (stage/get-ctx  stage))]
   [cdq.render.validate/do!]])

(defn do! [context]
  (utils/pipeline context pipeline))
