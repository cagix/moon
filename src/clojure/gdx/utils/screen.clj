(ns clojure.gdx.utils.screen
  (:import (com.badlogic.gdx.utils ScreenUtils)))

(defn clear! [color]
  (ScreenUtils/clear color))
