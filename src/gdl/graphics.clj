(ns gdl.graphics
  (:require [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.graphics.g2d.freetype :as freetype]
            [com.badlogic.gdx.math.vector3 :as vector3]
            gdl.graphics.bitmap-font)
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics OrthographicCamera)))

(defprotocol Graphics
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor])
  (cursor [_ pixmap hotspot-x hotspot-y])
  (clear! [_ [r g b a]]
          [_ r g b a])
  (texture [_ file-handle])
  (pixmap [_ width height pixmap-format]
          [_ file-handle])
  (fit-viewport [_ width height camera])
  (sprite-batch [_]))

(defn generate-font [file-handle params]
  (freetype/generate-font file-handle params))

(defn orthographic-camera
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
   (doto (orthographic-camera)
     (OrthographicCamera/.setToOrtho y-down? world-width world-height))))

(extend com.badlogic.gdx.graphics.g2d.BitmapFont
  gdl.graphics.bitmap-font/BitmapFont
  {:draw! bitmap-font/draw!})
