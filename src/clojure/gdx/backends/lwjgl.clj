(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx.input]
            [clojure.gdx.graphics]
            [com.badlogic.gdx.input :as input]
            [com.badlogic.gdx.input.buttons :as input-buttons]
            [com.badlogic.gdx.input.keys    :as input-keys]
            [clojure.gdx.application :as app]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as config])
  (:import (com.badlogic.gdx.graphics GL20)))

(defn create [listener config]
  (application/create (app/listener listener)
                      (config/create config)))

(extend-type com.badlogic.gdx.Input
  clojure.gdx.input/Input
  (button-just-pressed? [this button]
    {:pre [(contains? input-buttons/k->value button)]}
    (input/button-just-pressed? this (input-buttons/k->value button)))

  (key-pressed? [this key]
    (assert (contains? input-keys/k->value key)
            (str "(pr-str key): "(pr-str key)))
    (input/key-pressed? this (input-keys/k->value key)))

  (key-just-pressed? [this key]
    {:pre [(contains? input-keys/k->value key)]}
    (input/key-just-pressed? this (input-keys/k->value key)))

  (set-processor! [this input-processor]
    (input/set-processor! this input-processor))

  (mouse-position [this]
    [(input/x this)
     (input/y this)]))

(extend-type com.badlogic.gdx.Graphics
  clojure.gdx.graphics/Graphics
  (delta-time [graphics]
    (.getDeltaTime graphics))

  (frames-per-second [graphics]
    (.getFramesPerSecond graphics))

  (set-cursor! [graphics cursor]
    (.setCursor graphics cursor))

  (cursor [graphics pixmap hotspot-x hotspot-y]
    (.newCursor graphics pixmap hotspot-x hotspot-y))

  (clear! [graphics [r g b a]]
    (let [clear-depth? false
          apply-antialiasing? false
          gl20 (.getGL20 graphics)]
      (GL20/.glClearColor gl20 r g b a)
      (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                   clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                   (and apply-antialiasing? (.coverageSampling (.getBufferFormat graphics)))
                   (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
        (GL20/.glClear gl20 mask)))))
