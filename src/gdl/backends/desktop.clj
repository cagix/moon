(ns gdl.backends.desktop
  (:require [com.badlogic.gdx.input.keys :as input.keys]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]
            [com.badlogic.gdx.utils.os :as os]
            gdl.audio
            gdl.audio.sound
            gdl.files
            gdl.files.file-handle
            gdl.graphics
            gdl.input)
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx
                             Graphics
                             Input
                             Input$Buttons)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20)))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode (.setWindowedMode object
                                     (int (:width v))
                                     (int (:height v)))
    :title (.setTitle object (str v))))

(defn- set-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :foreground-fps (.setForegroundFPS object (int v))
    (set-window-config-key! object k v)))

(defn- config->java [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (set-config-key! obj k v))
    obj))

(defn- listener->java
  [{:keys [create
           dispose
           render
           resize
           pause
           resume]}]
  (reify ApplicationListener
    (create [_]
      (create {:gdx/app      Gdx/app
               :gdx/audio    Gdx/audio
               :gdx/files    Gdx/files
               :gdx/graphics Gdx/graphics
               :gdx/input    Gdx/input}))
    (dispose [_]
      (dispose))
    (render [_]
      (render))
    (resize [_ width height]
      (resize width height))
    (pause [_]
      (pause))
    (resume [_]
      (resume))))

(defn application
  [os->dispatches listener config]
  (doseq [[f params] (get os->dispatches (os/value->keyword shared-library-loader/os))]
    ((requiring-resolve f) params))
  (Lwjgl3Application. (listener->java listener)
                      (config->java config)))

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
