(ns clojure.gdx.graphics.texture.filter
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)))

(defn ->from-keyword [k]
  (case k
    :texture-filter/linear Texture$TextureFilter/Linear))
