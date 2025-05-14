(ns cdq.game.clear-screen
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn do! []
  (ScreenUtils/clear Color/BLACK))
