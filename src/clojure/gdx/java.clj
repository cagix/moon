(ns clojure.gdx.java
  (:require [clojure.gdx.graphics]
            [clojure.gdx.graphics.pixmap]
            [clojure.gdx.graphics.g2d.batch]
            [clojure.gdx.input]
            [clojure.gdx.utils.disposable]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Gdx
                             Graphics
                             Input)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d Batch
                                          SpriteBatch)))

(defprotocol JavaObjectState
  (get-state [_]))

(defn- reify-pixmap [^Pixmap this]
  (reify
    JavaObjectState
    (get-state [_]
      this)

    clojure.gdx.utils.disposable/Disposable
    (dispose! [_]
      (.dispose this))

    clojure.gdx.graphics.pixmap/Pixmap
    (set-color! [_ color]
      (.setColor this color))

    (draw-pixel! [_ x y]
      (.drawPixel this x y))))

(defn- reify-batch [^Batch this]
  (reify
    JavaObjectState
    (get-state [_]
      this)

    clojure.gdx.utils.disposable/Disposable
    (dispose! [_]
      (.dispose this))

    clojure.gdx.graphics.g2d.batch/Batch
    (set-color! [_ color]
      (.setColor this color))

    (draw! [_ texture-region {:keys [x y origin-x origin-y width height scale-x scale-y rotation]}]
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
             rotation))

    (begin! [_]
      (.begin this))

    (end! [_]
      (.end this))

    (set-projection-matrix! [_ matrix]
      (.setProjectionMatrix this matrix))))

(defn- reify-graphics [^Graphics this]
  (reify clojure.gdx.graphics/Graphics
    (delta-time [_]
      (.getDeltaTime this))

    (frames-per-second [_]
      (.getFramesPerSecond this))

    (cursor [_ pixmap hotspot-x hotspot-y]
      (.newCursor this (get-state pixmap) hotspot-x hotspot-y)) ; returns state

    (set-cursor! [_ cursor]
      (.setCursor this cursor))

    (pixmap [_ file-handle]
      (reify-pixmap (Pixmap. file-handle)))

    (pixmap [_ width height format]
      (reify-pixmap (Pixmap. width height (case format
                                            :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

    (texture [_ pixmap]
      (Texture. (get-state pixmap)))

    (sprite-batch [_]
      (reify-batch (SpriteBatch.)))))

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
                      clojure.gdx/graphics
                      clojure.gdx/input])

(defn context []
  (map->Context {:graphics (reify-graphics Gdx/graphics)
                 :input    (reify-input    Gdx/input)}))
