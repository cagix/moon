(ns gdl.graphics
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Texture
                                      Pixmap
                                      Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.utils Disposable)))

(defn color [r g b a]
  (Color. (float r)
          (float g)
          (float b)
          (float a)))

(def white Color/WHITE)
(def black Color/BLACK)

(defn def-markdown-color [name color]
  (Colors/put name color))

(defn texture-region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture x y w h]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h))))

(defn sub-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region
                  (int x)
                  (int y)
                  (int w)
                  (int h)) )

(defn dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn truetype-font [{:keys [file size quality-scaling]}]
  (let [font (freetype/generate (.internal Gdx/files file)
                                {:size (* size quality-scaling)})]
    (bitmap-font/configure! font {:scale (/ quality-scaling)
                                  :enable-markup? true
                                  :use-integer-positions? true}))) ; otherwise scaling to world-units not visible

(defn create-cursor [path hotspot-x hotspot-y]
  (let [pixmap (Pixmap. (.internal Gdx/files path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defprotocol Batch
  (draw-on-viewport! [_ viewport draw-fn])
  (draw-texture-region! [_ texture-region [x y] [w h] rotation color]))

(defn sprite-batch []
  (let [this (SpriteBatch.)]
    (reify
      Batch
      (draw-on-viewport! [_ viewport draw-fn]
        (.setColor this Color/WHITE) ; fix scene2d.ui.tooltip flickering
        (.setProjectionMatrix this (camera/combined (:camera viewport)))
        (.begin this)
        (draw-fn)
        (.end this))

      (draw-texture-region! [_ texture-region [x y] [w h] rotation color]
        (if color (.setColor this color))
        (.draw this
               texture-region
               x
               y
               (/ (float w) 2) ; rotation origin
               (/ (float h) 2)
               w
               h
               1 ; scale-x
               1 ; scale-y
               rotation)
        (if color (.setColor this Color/WHITE)))

      Disposable
      (dispose [_]
        (.dispose this))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this)))))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))
