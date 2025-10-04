(ns com.badlogic.gdx.graphics
  (:require [com.badlogic.gdx.utils.viewport.fit-viewport :as fit-viewport]
            gdl.graphics.batch)
  (:import (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Texture
                                      Pixmap
                                      Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn delta-time [^Graphics this]
  (.getDeltaTime this))
(defn frames-per-second [^Graphics this]
  (.getFramesPerSecond this))
(defn set-cursor! [^Graphics this cursor]
  (.setCursor this cursor))
(defn cursor [^Graphics this pixmap hotspot-x hotspot-y]
  (.newCursor this pixmap hotspot-x hotspot-y))
(defn clear!
  ([this [r g b a]]
   (clear! this r g b a))
  ([^Graphics this r g b a]
   (let [clear-depth? false
         apply-antialiasing? false
         gl20 (.getGL20 this)]
     (GL20/.glClearColor gl20 r g b a)
     (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                  clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                  (and apply-antialiasing? (.coverageSampling (.getBufferFormat this)))
                  (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
       (GL20/.glClear gl20 mask)))))
(defn texture [_ file-handle]
  (Texture. ^FileHandle file-handle))
(defn pixmap
  ([_ ^FileHandle file-handle]
   (Pixmap. file-handle))
  ([_ width height pixmap-format]
   (Pixmap. (int width)
            (int height)
            (case pixmap-format
              :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

(defn fit-viewport [_ width height camera]
  (fit-viewport/create width height camera))

(defn sprite-batch [_]
  (SpriteBatch.))

(extend-type SpriteBatch
  gdl.graphics.batch/Batch
  (draw! [this texture-region x y [w h] rotation]
    (.draw this
           texture-region
           x
           y
           (/ (float w) 2) ; origin-x
           (/ (float h) 2) ; origin-y
           w
           h
           1 ; scale-x
           1 ; scale-y
           rotation))

  (set-color! [this [r g b a]]
    (.setColor this r g b a))

  (set-projection-matrix! [this matrix]
    (.setProjectionMatrix this matrix))

  (begin! [this]
    (.begin this))

  (end! [this]
    (.end this)))
