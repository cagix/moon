(ns clojure.gdx)

(defprotocol Audio
  (sound [_ path]))

(defprotocol Graphics
  (sprite-batch [_]))
