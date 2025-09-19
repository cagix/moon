(ns com.badlogic.gdx.backends.lwjgl3.init.gdx
  (:require [com.badlogic.gdx.input.keys :as input.keys]
            gdl.audio
            gdl.audio.sound
            gdl.files
            gdl.files.file-handle
            gdl.graphics
            gdl.input)
  (:import (com.badlogic.gdx Audio
                             Files
                             Gdx
                             Graphics
                             Input
                             Input$Buttons)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20)))

(defn set-app! [{:keys [init/application] :as init}]
  (set! Gdx/app application)
  init)

(defn set-audio! [{:keys [init/application]
                   :as init}]
  (set! Gdx/audio (.audio application))
  init)

(defn set-files! [{:keys [init/application]
                   :as init}]
  (set! Gdx/files (.files application))
  init)

(defn set-net! [{:keys [init/application]
                 :as init}]
  (set! Gdx/net (.net application))
  init)

(extend-type Audio
  gdl.audio/Audio
  (sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  gdl.audio.sound/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))

(extend-type Files
  gdl.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type FileHandle
  gdl.files.file-handle/FileHandle
  (list [this]
    (.list this))
  (directory? [this]
    (.isDirectory this))
  (extension [this]
    (.extension this))
  (path [this]
    (.path this)))

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
         (GL20/.glClear gl20 mask))))))

(def ^:private buttons-k->value
  {:back    Input$Buttons/BACK
   :forward Input$Buttons/FORWARD
   :left    Input$Buttons/LEFT
   :middle  Input$Buttons/MIDDLE
   :right   Input$Buttons/RIGHT})

(extend-type Input
  gdl.input/Input
  (button-just-pressed? [this button]
    {:pre [(contains? buttons-k->value button)]}
    (.isButtonJustPressed this (buttons-k->value button)))

  (key-pressed? [this key]
    (assert (contains? input.keys/k->value key)
            (str "(pr-str key): "(pr-str key)))
    (.isKeyPressed this (input.keys/k->value key)))

  (key-just-pressed? [this key]
    {:pre [(contains? input.keys/k->value key)]}
    (.isKeyJustPressed this (input.keys/k->value key)))

  (set-processor! [this input-processor]
    (.setInputProcessor this input-processor))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))
