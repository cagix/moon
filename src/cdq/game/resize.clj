(ns cdq.game.resize
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn do! [{:keys [ctx/graphics
                   ctx/ui-viewport]} width height]
  (Viewport/.update ui-viewport width height true)
  (Viewport/.update (:g/world-viewport graphics) width height false))
