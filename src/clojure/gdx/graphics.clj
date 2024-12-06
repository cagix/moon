(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn pixmap
  (^Pixmap [^FileHandle file-handle]
   (Pixmap. file-handle))
  (^Pixmap [width height]
   (Pixmap. (int width) (int height) Pixmap$Format/RGBA8888)))

(defn orthographic-camera ^OrthographicCamera []
  (OrthographicCamera.))

(defn clear-screen [color]
  (ScreenUtils/clear color))

(defn texture [^Pixmap pixmap]
  (Texture. pixmap))

(defn texture-region [^Texture texture x y w h]
  (TextureRegion. texture (int x) (int y) (int w) (int h)))

(defn sprite-batch []
  (SpriteBatch.))
