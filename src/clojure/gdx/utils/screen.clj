(ns clojure.gdx.utils.screen
  (:import (com.badlogic.gdx.utils ScreenUtils)))

(defn clear! [[r g b a]]
  (ScreenUtils/clear r g b a))
