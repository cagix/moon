(ns clojure.gdx.graphics
  (:require clojure.graphics)
  (:import (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Texture
                                      Pixmap
                                      Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(extend-type com.badlogic.gdx.Graphics
  clojure.graphics/Graphics
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
     (clojure.graphics/clear! this r g b a))

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

  (sprite-batch [_]
    (SpriteBatch.)))
