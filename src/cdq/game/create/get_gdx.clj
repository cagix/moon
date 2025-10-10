(ns cdq.game.create.get-gdx
  (:require [cdq.audio]
            [clojure.input]
            [clojure.gdx :as gdx]
            [clojure.gdx.graphics]
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
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defrecord Context [^Files files
                    ^Graphics graphics]
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

  )

(defn- load-sound [path]
  (.newSound Gdx/audio (.internal Gdx/files path)))

(defn- create-audio
  [{:keys [sound-names path-format]}]
  (let [sounds (into {}
                     (for [sound-name sound-names]
                       [sound-name
                        (->> sound-name
                             (format path-format)
                             load-sound)]))]
    (reify cdq.audio/Audio
      (sound-names [_]
        (map first sounds))

      (play! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (Sound/.play (get sounds sound-name)))

      (dispose! [_]
        (run! Sound/.dispose (vals sounds))))))

(defn do! [ctx config]
  (assoc ctx
         :ctx/gdx (map->Context
                   {:files    Gdx/files
                    :graphics Gdx/graphics})
         :ctx/audio (create-audio (:audio config))
         :ctx/input Gdx/input))

(extend-type Input
  clojure.input/Input
  (set-processor! [this input-processor]
    (.setInputProcessor this input-processor))

  (key-pressed? [this key]
    (.isKeyPressed this key))

  (key-just-pressed? [this key]
    (.isKeyJustPressed this key))

  (button-just-pressed? [this button]
    (.isButtonJustPressed this button))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))
