(ns cdq.application.render
  (:require [cdq.application.render.try-fetch-state-ctx        :as render.try-fetch-state-ctx]
            [cdq.application.render.validate                   :as render.validate]
            [cdq.application.render.update-mouse               :as render.update-mouse]
            [cdq.application.render.update-mouseover-eid       :as render.update-mouseover-eid]
            [cdq.application.render.check-open-debug           :as render.check-open-debug]
            [cdq.application.render.assoc-active-entities      :as render.assoc-active-entities]
            [cdq.application.render.set-camera-on-player       :as render.set-camera-on-player]
            [cdq.application.render.draw-game                  :as render.draw-game]
            [cdq.application.render.assoc-interaction-state    :as render.assoc-interaction-state]
            [cdq.application.render.set-cursor                 :as render.set-cursor]
            [cdq.application.render.player-state-handle-input  :as render.player-state-handle-input]
            [cdq.application.render.remove-interaction-state   :as render.remove-interaction-state]
            [cdq.application.render.assoc-paused               :as render.assoc-paused]
            [cdq.application.render.update-world-time          :as render.update-world-time]
            [cdq.application.render.update-potential-fields    :as render.update-potential-fields]
            [cdq.application.render.tick-entities              :as render.tick-entities]
            [cdq.application.render.remove-destroyed-entities  :as render.remove-destroyed-entities]
            [cdq.application.render.window-and-camera-controls :as render.window-and-camera-controls]
            [cdq.application.render.render-stage               :as render.render-stage]))

(defn do! [ctx]
  (-> ctx
      render.try-fetch-state-ctx/do!
      render.validate/do!
      render.update-mouse/do!
      render.update-mouseover-eid/do!
      render.check-open-debug/do!
      render.assoc-active-entities/do!
      render.set-camera-on-player/do!
      render.draw-game/do!
      render.assoc-interaction-state/do!
      render.set-cursor/do!
      render.player-state-handle-input/do!
      render.remove-interaction-state/do!
      render.assoc-paused/do!
      render.update-world-time/do!
      render.update-potential-fields/do!
      render.tick-entities/do!
      render.remove-destroyed-entities/do!
      render.window-and-camera-controls/do!
      render.render-stage/do!
      render.validate/do!))
