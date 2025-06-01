(ns gdl.assets)

(defprotocol Assets
  (sound [_ path])
  (texture [_ path])
  (all-sounds [_])
  (all-textures [_]))
