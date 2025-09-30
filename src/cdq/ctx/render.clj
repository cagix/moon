(ns cdq.ctx.render
  (:require [cdq.ctx.assoc-active-entities :as assoc-active-entities]
            [cdq.ctx.assoc-paused :as assoc-paused]
            [cdq.ctx.assoc-interaction-state :as assoc-interaction-state]
            [cdq.ctx.clear-screen :as clear-screen]
            [cdq.ctx.check-open-debug :as check-open-debug]
            [cdq.ctx.dissoc-interaction-state :as dissoc-interaction-state]
            [cdq.ctx.draw-on-world-viewport :as draw-on-world-viewport]
            [cdq.ctx.draw-world-map :as draw-world-map]
            [cdq.ctx.get-stage-ctx :as get-stage-ctx]
            [cdq.ctx.render-stage :as render-stage]
            [cdq.ctx.remove-destroyed-entities :as remove-destroyed-entities]
            [cdq.ctx.set-camera-on-player :as set-camera-on-player]
            [cdq.ctx.set-cursor :as set-cursor]
            [cdq.ctx.tick-entities :as tick-entities]
            [cdq.ctx.player-state-handle-input :as player-state-handle-input]
            [cdq.ctx.update-mouse :as update-mouse]
            [cdq.ctx.update-mouseover-eid :as update-mouseover-eid]
            [cdq.ctx.update-potential-fields :as update-potential-fields]
            [cdq.ctx.update-world-time :as update-world-time]
            [cdq.ctx.validate :as validate]
            [cdq.ctx.window-camera-controls :as window-camera-controls]))

(defn do! [ctx]
  (-> ctx
      get-stage-ctx/do!
      validate/do!
      update-mouse/do!
      update-mouseover-eid/do!
      check-open-debug/do!
      assoc-active-entities/do!
      set-camera-on-player/do!
      clear-screen/do!
      draw-world-map/do!
      draw-on-world-viewport/do!
      assoc-interaction-state/do!
      set-cursor/do!
      player-state-handle-input/do!
      dissoc-interaction-state/do!
      assoc-paused/do!
      update-world-time/do!
      update-potential-fields/do!
      tick-entities/do!
      remove-destroyed-entities/do!
      window-camera-controls/do!
      render-stage/do!
      validate/do!))
