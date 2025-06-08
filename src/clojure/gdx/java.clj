(ns clojure.gdx.java
  (:require [clojure.gdx.app]
            [clojure.gdx.audio]
            [clojure.gdx.audio.sound]
            [clojure.gdx.files]
            [clojure.gdx.files.file-handle]
            [clojure.gdx.graphics]
            [clojure.gdx.input]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Application
                             Audio
                             Files
                             Gdx
                             Graphics
                             Input)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format)))

(defprotocol JavaObjectState
  (get-state [_]))

(defn- reify-app [^Application this]
  (reify clojure.gdx.app/Application
    (post-runnable! [_ runnable]
      (.postRunnable this runnable))))

(defn- reify-sound [^Sound this]
  (reify clojure.gdx.audio.sound/Sound
    (play! [_]
      (.play this))))

(defn- reify-audio [^Audio this]
  (reify clojure.gdx.audio/Audio
    (sound [_ file-handle]
      (reify-sound (.newSound this (get-state file-handle))))))

(defn- reify-file-handle [^FileHandle this]
  (reify
    JavaObjectState
    (get-state [_]
      this)

    clojure.gdx.files.file-handle/FileHandle
    (list [_]
      (map reify-file-handle (.list this)))

    (directory? [_]
      (.isDirectory this))

    (extension [_]
      (.extension this))

    (path [_]
      (.path this))))

(defn- reify-files [^Files this]
  (reify clojure.gdx.files/Files
    (internal [_ path]
      (reify-file-handle (.internal this path)))))

(defn- reify-graphics [^Graphics this]
  (reify clojure.gdx.graphics/Graphics
    (delta-time [_]
      (.getDeltaTime this))

    (frames-per-second [_]
      (.getFramesPerSecond this))

    (cursor [_ pixmap hotspot-x hotspot-y]
      (.newCursor this pixmap hotspot-x hotspot-y)) ; returns state

    (set-cursor! [_ cursor]
      (.setCursor this cursor))

    (pixmap [_ file-handle]
      (Pixmap. ^FileHandle (get-state file-handle)))

    (pixmap [_ width height format]
      (Pixmap. width height (case format
                              :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888)))))

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

(q/defrecord Context [clojure.gdx/app
                      clojure.gdx/audio
                      clojure.gdx/files
                      clojure.gdx/graphics
                      clojure.gdx/input])

(defn context []
  (map->Context {:app      (reify-app      Gdx/app)
                 :audio    (reify-audio    Gdx/audio)
                 :files    (reify-files    Gdx/files)
                 :graphics (reify-graphics Gdx/graphics)
                 :input    (reify-input    Gdx/input)}))
