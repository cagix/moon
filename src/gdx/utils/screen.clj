(ns gdx.utils.screen
  (:require [gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.utils ScreenUtils)))

(defn clear! [color]
  (ScreenUtils/clear (color/->obj color)))
