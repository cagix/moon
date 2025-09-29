(ns gdl.graphics
  (:require [com.badlogic.gdx.math.vector3 :as vector3]
            [gdl.utils.viewport.fit-viewport :as fit-viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defprotocol PGraphics
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

(extend-type Graphics
  PGraphics
  (delta-time [this]
    (.getDeltaTime this))
  (frames-per-second [this]
    (.getFramesPerSecond this))
  (set-cursor! [this cursor]
    (.setCursor this cursor))
  (cursor [this pixmap hotspot-x hotspot-y]
    (.newCursor this pixmap hotspot-x hotspot-y))
  (clear!
    ([this [r g b a]]
     (clear! this r g b a))
    ([this r g b a]
     (let [clear-depth? false
           apply-antialiasing? false
           gl20 (.getGL20 this)]
       (GL20/.glClearColor gl20 r g b a)
       (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                    clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                    (and apply-antialiasing? (.coverageSampling (.getBufferFormat this)))
                    (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
         (GL20/.glClear gl20 mask)))))
  (texture [_ file-handle]
    (Texture. ^FileHandle file-handle))
  (pixmap
    ([_ ^FileHandle file-handle]
     (Pixmap. file-handle))
    ([_ width height pixmap-format]
     (Pixmap. (int width)
              (int height)
              (case pixmap-format
                :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

  (fit-viewport [_ width height camera]
    (fit-viewport/create width height camera))

  (sprite-batch [_]
    (SpriteBatch.)))

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
