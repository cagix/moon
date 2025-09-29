(ns com.badlogic.gdx.graphics.orthographic-camera
  (:require [com.badlogic.gdx.math.vector3 :as vector3])
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

(defn set-position! [^OrthographicCamera this [x y]]
  (set! (.x (.position this)) (float x))
  (set! (.y (.position this)) (float y))
  (.update this))

(defn set-zoom! [^OrthographicCamera this amount]
  (set! (.zoom this) amount)
  (.update this))
