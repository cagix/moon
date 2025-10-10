(ns clojure.gdx.viewport
  (:require [clojure.gdx.math.vector2 :as vector2]))

(defprotocol Viewport
  (camera [_])
  (update! [_ width height {:keys [center?]}])
  (world-width [_])
  (world-height [_])
  (unproject [_ [x y]]))

(extend-type com.badlogic.gdx.utils.viewport.Viewport
  Viewport
  (camera [this]
    (.getCamera this))

  (update! [viewport width height {:keys [center?]}]
    (.update viewport width height (boolean center?)))

  (world-width [this]
    (.getWorldWidth this))

  (world-height [this]
    (.getWorldHeight this))

  (unproject [viewport [x y]]
    (-> viewport
        (.unproject (vector2/->java x y))
        vector2/->clj)))
