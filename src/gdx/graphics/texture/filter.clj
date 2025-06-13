(ns gdx.graphics.texture.filter
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)))

(let [mapping {:linear Texture$TextureFilter/Linear}]
  (defn k->value [k]
    (when-not (contains? mapping k)
      (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys mapping))))))
    (k mapping)))
