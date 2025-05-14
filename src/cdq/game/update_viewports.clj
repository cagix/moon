(ns cdq.game.update-viewports
  (:require [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn do! [width height]
  (Viewport/.update (:ui-viewport    ctx/graphics) width height true)
  (Viewport/.update (:world-viewport ctx/graphics) width height false))
