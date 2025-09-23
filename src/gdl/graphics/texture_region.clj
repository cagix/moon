(ns gdl.graphics.texture-region)

(defprotocol TextureRegion
  (dimensions [_] "Returns `[width height]`."))
