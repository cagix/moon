(ns gdx.graphics.g2d
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)))

(defn sprite-batch []
  (SpriteBatch.))

(defn texture-region
  ([texture]
   (TextureRegion. texture))
  ([texture-region x y w h]
   (TextureRegion. texture-region
                   (int x)
                   (int y)
                   (int w)
                   (int h))))
