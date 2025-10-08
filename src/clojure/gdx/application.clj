(ns clojure.gdx.application
  (:require [cdq.graphics.color :as color]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.bitmap-font :as bitmap-font]
            [clojure.gdx.shape-drawer :as shape-drawer]
            [clojure.string :as str]
            [clojure.math :as math])
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx
                             Graphics)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Texture
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils Align)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(extend-type ShapeDrawer
  shape-drawer/ShapeDrawer
  (arc! [this [center-x center-y] radius start-angle degree color]
    (.setColor this (color/float-bits color))
    (.arc this
          center-x
          center-y
          radius
          (math/to-radians start-angle)
          (math/to-radians degree)) )

  (circle! [this [x y] radius color]
    (.setColor this (color/float-bits color))
    (.circle this x y radius))

  (ellipse! [this [x y] radius-x radius-y color]
    (.setColor this (color/float-bits color))
    (.ellipse this x y radius-x radius-y))

  (filled-ellipse! [this [x y] radius-x radius-y color]
    (.setColor this (color/float-bits color))
    (.filledEllipse this x y radius-x radius-y) )

  (filled-circle! [this [x y] radius color]
    (.setColor this (color/float-bits color))
    (.filledCircle this (float x) (float y) (float radius)))

  (filled-rectangle! [this x y w h color]
    (.setColor this (color/float-bits color))
    (.filledRectangle this (float x) (float y) (float w) (float h)) )

  (line! [this [sx sy] [ex ey] color]
    (.setColor this (color/float-bits color))
    (.line this (float sx) (float sy) (float ex) (float ey)))

  (rectangle! [this x y w h color]
    (.setColor this (color/float-bits color))
    (.rectangle this x y w h) )

  (sector! [this [center-x center-y] radius start-angle degree color]
    (.setColor this (color/float-bits color))
    (.sector this
             center-x
             center-y
             radius
             (math/to-radians start-angle)
             (math/to-radians degree))))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(extend-type BitmapFont
  bitmap-font/BitmapFont
  (draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
    (let [old-scale (.scaleX (.getData font))]
      (.setScale (.getData font) (* old-scale scale))
      (.draw font
             batch
             text
             (float x)
             (float (+ y (if up? (text-height font text) 0)))
             (float target-width)
             (or h-align Align/center)
             wrap?)
      (.setScale (.getData font) old-scale))))

(extend-type Sound
  audio/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))

(defrecord Context [^Audio audio
                    ^Files files
                    ^Graphics graphics
                    input]
  gdx/Audio
  (sound [_ path]
    (.newSound audio (.internal files path)))

  gdx/Graphics
  (sprite-batch [_]
    (SpriteBatch.))

  (cursor [_ path [hotspot-x hotspot-y]]
    (let [pixmap (Pixmap. (.internal files path))
          cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
      (.dispose pixmap)
      cursor))

  (truetype-font [_ path {:keys [size
                                 quality-scaling
                                 enable-markup?
                                 use-integer-positions?]}]
    (let [generator (FreeTypeFontGenerator. (.internal files path))
          font (.generateFont generator
                              (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
                                (set! (.size params) (* size quality-scaling))
                                (set! (.minFilter params) Texture$TextureFilter/Linear)
                                (set! (.magFilter params) Texture$TextureFilter/Linear)
                                params))]
      (.setScale (.getData font) (/ quality-scaling))
      (set! (.markupEnabled (.getData font)) enable-markup?)
      (.setUseIntegerPositions font use-integer-positions?)
      font))

  (shape-drawer [_ batch texture-region]
    (ShapeDrawer. batch texture-region))

  (texture [_ path]
    (Texture. (.internal files path)))
  )

(defn start!
  [{:keys [title
           window
           fps
           create!
           dispose!
           render!
           resize!
           colors]}]
  (doseq [[name [r g b a]] colors]
    (Colors/put name (Color. r g b a)))
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create! (map->Context
                                    {:audio    Gdx/audio
                                     :files    Gdx/files
                                     :graphics Gdx/graphics
                                     :input    Gdx/input})))

                        (dispose [_]
                          (dispose!))

                        (render [_]
                          (render!))

                        (resize [_ width height]
                          (resize! width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width window)
                                          (:height window))
                        (.setForegroundFPS fps))))
