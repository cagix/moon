(ns clojure.graphics
  (:import (com.badlogic.gdx Gdx)))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))
