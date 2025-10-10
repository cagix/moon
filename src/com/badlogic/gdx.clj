(ns com.badlogic.gdx
  (:require [clojure.audio]
            [clojure.input]
            [clojure.sound]
            [clojure.gdx :as gdx]
            [clojure.gdx.graphics]
            [clojure.gdx.bitmap-font :as bitmap-font]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Audio
                             Files
                             Gdx
                             Graphics
                             Input)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Pixmap
                                      Texture
                                      Texture$TextureFilter
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils Align)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defrecord Context [^Audio audio
                    ^Files files
                    ^Graphics graphics
                    ^Input input]
  clojure.audio/Audio
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

  (texture [_ path]
    (Texture. (.internal files path)))

  (viewport [_ world-width world-height camera]
    (FitViewport. world-width world-height camera))

  (delta-time [_]
    (.getDeltaTime graphics))

  (frames-per-second [_]
    (.getFramesPerSecond graphics))

  (set-cursor! [_ cursor]
    (.setCursor graphics cursor))

  (clear! [_ color]
    (clojure.gdx.graphics/clear! graphics color))

  (orthographic-camera [_]
    (OrthographicCamera.))

  (orthographic-camera[_ {:keys [y-down? world-width world-height]}]
    (doto (OrthographicCamera.)
      (.setToOrtho y-down? world-width world-height)))

  clojure.input/Input
  (set-processor! [_ input-processor]
    (.setInputProcessor input input-processor))

  (key-pressed? [_ key]
    (.isKeyPressed input key))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed input key))

  (button-just-pressed? [_ button]
    (.isButtonJustPressed input button))

  (mouse-position [_]
    [(.getX input)
     (.getY input)]))

(defn context []
  (map->Context
   {:audio    Gdx/audio
    :files    Gdx/files
    :graphics Gdx/graphics
    :input    Gdx/input}))

(extend-type Sound
  clojure.sound/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))

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
