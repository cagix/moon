(ns clojure.graphics.tiled-map-renderer)

(defprotocol TiledMapRenderer
  (draw [_ tiled-map color-setter camera]))
