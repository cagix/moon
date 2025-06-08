(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.graphics Texture)))

(defn load! [^String path]
  (Texture. path))
