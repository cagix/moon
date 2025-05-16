(ns cdq.game.update-viewports
  (:require [cdq.ctx :as ctx]
            [gdl.graphics.viewport :as viewport]))

(defn do! []
  (viewport/update! ctx/ui-viewport)
  (viewport/update! ctx/world-viewport))
