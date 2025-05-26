(ns gdl.graphics
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Texture
                                      Pixmap)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

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
