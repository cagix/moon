(ns clojure.gdx.java
  (:require [clojure.gdx.app]
            [clojure.gdx.audio]
            [clojure.gdx.files]
            [clojure.gdx.graphics]
            [clojure.gdx.input]
            )
  (:import (com.badlogic.gdx Application
                             Audio
                             Files
                             Gdx
                             Graphics
                             Input
                             )))

(defn- reify-app [^Application this]
  (reify clojure.gdx.app/Application
    (post-runnable! [_ runnable]
      (.postRunnable this runnable))))

(defn- reify-audio [^Audio this]
  (reify clojure.gdx.audio/Audio
    (sound [_ file-handle]
      (.newSound this file-handle))))

(defn- reify-files [^Files this]
  (reify clojure.gdx.files/Files
    (internal [_ path]
      (.internal this path))))

(defn- reify-graphics [^Graphics this]
  (reify clojure.gdx.graphics/Graphics
    (delta-time [_]
      (.getDeltaTime this))

    (frames-per-second [_]
      (.getFramesPerSecond this))

    (cursor [_ pixmap hotspot-x hotspot-y]
      (.newCursor this pixmap hotspot-x hotspot-y))

    (set-cursor! [_ cursor]
      (.setCursor this cursor))))

(defn- reify-input [^Input this]
  (reify clojure.gdx.input/Input
    (button-just-pressed? [_ button]
      (.isButtonJustPressed this button))

    (key-pressed? [_ key]
      (.isKeyPressed this key))

    (key-just-pressed? [_ key]
      (.isKeyJustPressed this key))

    (set-processor! [_ input-processor]
      (.setInputProcessor this input-processor))

    (x [_]
      (.getX this))

    (y [_]
      (.getY this))))

(defn context []
  {:clojure.gdx/app      (reify-app      Gdx/app)
   :clojure.gdx/audio    (reify-audio    Gdx/audio)
   :clojure.gdx/files    (reify-files    Gdx/files)
   :clojure.gdx/graphics (reify-graphics Gdx/graphics)
   :clojure.gdx/input    (reify-input    Gdx/input)})
