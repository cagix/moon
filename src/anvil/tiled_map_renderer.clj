(ns anvil.tiled-map-renderer)

(defprotocol TiledMapRenderer
  (render [_ tiled-map color-setter camera]))
