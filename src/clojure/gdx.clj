(ns clojure.gdx
  (:require [clojure.files]
            [clojure.files.file-handle]
            [clojure.gdx.interop :refer [k->input-button k->input-key]]
            [clojure.graphics]
            [clojure.graphics.2d.batch]
            [clojure.input]
            [clojure.java.io :as io]
            [gdl.audio]
            [gdl.utils])
  (:import (com.badlogic.gdx Gdx)))

(defn context []
  {::app      Gdx/app
   ::audio    Gdx/audio
   ::files    Gdx/files
   ::gl       Gdx/gl
   ::gl20     Gdx/gl20
   ::gl30     Gdx/gl30
   ::gl31     Gdx/gl31
   ::gl32     Gdx/gl32
   ::graphics Gdx/graphics
   ::input    Gdx/input
   ::net      Gdx/net})

(extend-type com.badlogic.gdx.Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type com.badlogic.gdx.Graphics
  clojure.graphics/Graphics
  (new-cursor [this pixmap hotspot-x hotspot-y]
    (.newCursor this pixmap hotspot-x hotspot-y))
  (set-cursor [this cursor]
    (.setCursor this cursor)))

(extend-type com.badlogic.gdx.files.FileHandle
  clojure.files.file-handle/FileHandle
  (list [this]
    (.list this))
  (directory? [this]
    (.isDirectory this))
  (extension [this]
    (.extension this))
  (path [this]
    (.path this)))

(extend-type com.badlogic.gdx.Input
  clojure.input/Input
  (x [this]
    (.getX this))

  (y [this]
    (.getY this))

  (button-just-pressed? [this button]
    (.isButtonJustPressed this (k->input-button button)))

  (key-just-pressed? [this key]
    (.isKeyJustPressed this (k->input-key key)))

  (key-pressed? [this key]
    (.isKeyPressed this (k->input-key key)))

  (set-processor [this input-processor]
    (.setInputProcessor this input-processor)))

(extend-type com.badlogic.gdx.audio.Sound
  gdl.audio/Sound
  (play [this]
    (.play this)))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.utils/Disposable
  (dispose [this]
    (.dispose this)))

(extend-type com.badlogic.gdx.graphics.g2d.Batch
  clojure.graphics.2d.batch/Batch
  (set-projection-matrix [this projection]
    (.setProjectionMatrix this projection))
  (begin [this]
    (.begin this))
  (end
    [this]
    (.end this))
  (set-color [this color]
    (.setColor this color))
  (draw [this texture-region {:keys [x y origin-x origin-y width height scale-x scale-y rotation]}]
    (.draw this
           texture-region
           x
           y
           origin-x
           origin-y
           width
           height
           scale-x
           scale-y
           rotation)))
