(ns cdq.game.update-viewports
  (:require [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.utils.viewport Viewport)))

(defn do! []
  (let [width  (.getWidth  Gdx/graphics)
        height (.getHeight Gdx/graphics)]
    (Viewport/.update ctx/ui-viewport    width height true)
    (Viewport/.update ctx/world-viewport width height false)))
