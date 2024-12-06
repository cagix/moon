(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn pixmap
  ([file-handle]
   (Pixmap. file-handle))
  ([width height]
   (Pixmap. width height Pixmap$Format/RGBA8888)))

(defn orthographic-camera []
  (OrthographicCamera.))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(defn clear-screen [color]
  (ScreenUtils/clear color))

(defn texture [pixmap]
  (Texture. pixmap))

(defn texture-region [texture x y w h]
  (TextureRegion. ^Texture texture x y w h))
