(ns clojure.assets)

(defprotocol Assets
  (all-sounds [_])
  (all-textures [_]))
