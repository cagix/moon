(ns gdl.graphics.g2d.texture-region)

(defprotocol TextureRegion
  (dimensions [_])
  (region [_ x y w h]))
