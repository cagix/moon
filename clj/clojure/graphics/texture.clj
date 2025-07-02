(ns clojure.graphics.texture)

(defprotocol Texture
  (region [_]
          [_ x y w h]
          "Returns a [[clojure.graphics.g2d.texture-region/TextureRegion]]"))
