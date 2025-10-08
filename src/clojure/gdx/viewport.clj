(ns clojure.gdx.viewport)

(defprotocol Viewport
  (camera [_])
  (update! [_ width height {:keys [center?]}])
  (world-width [_])
  (world-height [_])
  (unproject [_ [x y]]))

