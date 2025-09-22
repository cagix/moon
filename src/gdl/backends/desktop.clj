(ns gdl.backends.desktop
  (:require clojure.java.awt.taskbar
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [com.badlogic.gdx.input.buttons :as input.buttons]
            [com.badlogic.gdx.input.keys :as input.keys]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]
            gdl.audio
            gdl.audio.sound
            gdl.files
            gdl.files.file-handle
            gdl.graphics
            gdl.input
            org.lwjgl.system.configuration)
  (:import (com.badlogic.gdx Audio
                             Files
                             Graphics
                             Input)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20)))

; 1. 'com.badlogic.gdx/get-state'
; 2. 'com.badlogic.gdx.application/listener'
; 3. keep one repo - delete unused repos/archive?

; FIXME does _2_ things


(defn- set-mac-os-settings!
  [{:keys [glfw-async?
           taskbar-icon]}]
  (when glfw-async?
    (org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when taskbar-icon
    (clojure.java.awt.taskbar/set-icon-image! taskbar-icon)))

(defn application
  [config]
  (when (= (shared-library-loader/operating-system) :mac)
    (set-mac-os-settings! (:mac config)))
  (lwjgl/start-application! (:listener config)
                            (dissoc config :mac :listener)))

; FIXME does _x_ things!!

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

(extend-type Input
  gdl.input/Input
  (button-just-pressed? [this button]
    {:pre [(contains? input.buttons/k->value button)]}
    (.isButtonJustPressed this (input.buttons/k->value button)))

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
