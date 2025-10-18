(ns cdq.game.render
  (:require cdq.game.render.validate
            cdq.game.render.get-stage-ctx
            cdq.game.render.update-mouse
            cdq.game.render.update-mouseover-eid
            cdq.game.render.check-open-debug
            cdq.game.render.assoc-active-entities
            cdq.game.render.set-camera-on-player
            cdq.game.render.assoc-paused
            cdq.game.render.clear-screen
            cdq.game.render.draw-world-map
            cdq.game.render.draw-on-world-viewport
            cdq.game.render.assoc-interaction-state
            cdq.game.render.set-cursor
            cdq.game.render.player-state-handle-input
            cdq.game.render.update-potential-fields
            cdq.game.render.tick-entities
            cdq.game.render.update-world-time
            cdq.game.render.remove-destroyed-entities
            cdq.game.render.window-camera-controls
            cdq.game.render.render-stage))

(defn do! [ctx]
  (-> ctx
      cdq.game.render.get-stage-ctx/step
      cdq.game.render.validate/step
      cdq.game.render.update-mouse/step
      cdq.game.render.update-mouseover-eid/step
      cdq.game.render.check-open-debug/step
      cdq.game.render.assoc-active-entities/step
      cdq.game.render.set-camera-on-player/step
      cdq.game.render.clear-screen/step
      cdq.game.render.draw-world-map/step
      cdq.game.render.draw-on-world-viewport/step
      cdq.game.render.assoc-interaction-state/step
      cdq.game.render.set-cursor/step
      cdq.game.render.player-state-handle-input/step
      (dissoc :ctx/interaction-state)
      cdq.game.render.assoc-paused/step
      cdq.game.render.update-world-time/step
      cdq.game.render.update-potential-fields/step
      cdq.game.render.tick-entities/step
      cdq.game.render.remove-destroyed-entities/step
      cdq.game.render.window-camera-controls/step
      cdq.game.render.render-stage/step
      cdq.game.render.validate/step))
