(ns clojure.gdx.graphics.orthographic-camera
  (:require [clojure.gdx.math.vector3 :as vector3]
            [clojure.graphics.orthographic-camera])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create
  ([]
   (proxy [OrthographicCamera ILookup] []
     (valAt [k]
       (let [^OrthographicCamera this this]
         (case k
           :camera/combined (.combined this)
           :camera/zoom (.zoom this)
           :camera/frustum {:frustum/plane-points (mapv vector3/clojurize (.planePoints (.frustum this)))}
           :camera/position (vector3/clojurize (.position this))
           :camera/viewport-width  (.viewportWidth  this)
           :camera/viewport-height (.viewportHeight this))))))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (create)
     (OrthographicCamera/.setToOrtho y-down? world-width world-height))))

(extend-type OrthographicCamera
  clojure.graphics.orthographic-camera/Camera
  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this)))
