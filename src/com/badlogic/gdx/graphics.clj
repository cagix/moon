(ns com.badlogic.gdx.graphics
  (:require gdl.graphics
            gdl.graphics.pixmap
            gdl.graphics.texture
            gdl.graphics.texture-region
            [com.badlogic.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)))

(extend-type Graphics
  gdl.graphics/Graphics
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
     (gdl.graphics/clear! this r g b a))
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
  (texture [this file-handle]
    (Texture. ^FileHandle file-handle))
  (pixmap
    ([this ^FileHandle file-handle]
     (Pixmap. file-handle))
    ([this width height pixmap-format]
     (Pixmap. (int width)
              (int height)
              (case pixmap-format
                :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

  (fit-viewport [_ width height camera]
    (fit-viewport/create width height camera))

  (sprite-batch [_]
    (SpriteBatch.)))

(extend-type Texture
  gdl.graphics.texture/Texture
  (region
    ([this]
     (TextureRegion. this))
    ([this [x y w h]]
     (TextureRegion. this
                     (int x)
                     (int y)
                     (int w)
                     (int h)))
    ([this x y w h]
     (TextureRegion. this
                     (int x)
                     (int y)
                     (int w)
                     (int h)))))

(extend-type TextureRegion
  gdl.graphics.texture-region/TextureRegion
  (dimensions [this]
    [(.getRegionWidth  this)
     (.getRegionHeight this)]))

(extend-type Pixmap
  gdl.graphics.pixmap/Pixmap
  (set-color! [this [r g b a]]
    (.setColor this r g b a))

  (draw-pixel! [this x y]
    (.drawPixel this x y))

  (texture [this]
    (Texture. this)))
