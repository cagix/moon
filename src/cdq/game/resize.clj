(ns cdq.game.resize
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn do! [{:keys [ctx/graphics]} width height]
  (Viewport/.update (:g/ui-viewport    graphics) width height true)
  (Viewport/.update (:g/world-viewport graphics) width height false))
