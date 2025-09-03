(ns cdq.render.clear-screen
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn do!
  [_ctx]
  (ScreenUtils/clear Color/BLACK))
