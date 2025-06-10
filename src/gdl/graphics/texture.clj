(ns gdl.graphics.texture)

(defprotocol Texture
  (region [_]
          [_ x y w h]
          "Returns a [[gdl.graphics.g2d.texture-region/TextureRegion]]"))
