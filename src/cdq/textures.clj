(ns cdq.textures
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

; FIXME this can be memoized
; also good for tiled-map tiles they have to be memoized too
(defn image->texture-region
  [textures {:keys [image/file image/bounds]}]
  (assert file)
  (assert (contains? textures file))
  (let [^Texture texture (get textures file)
        [x y w h] bounds]
    (if bounds
      (TextureRegion. texture
                      (int x)
                      (int y)
                      (int w)
                      (int h))
      (TextureRegion. texture))))
