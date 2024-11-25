(ns gdl.graphics.texture
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn region [^TextureRegion texture-region [x y w h]]
  (TextureRegion. texture-region (int x) (int y) (int w) (int h)))
