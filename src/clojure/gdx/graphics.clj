(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap OrthographicCamera)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn pixmap [file-handle] ; TODO filehandle?! reflection?
  (Pixmap. file-handle))

(defn orthographic-camera []
  (OrthographicCamera.))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(defn clear-screen [color]
  (ScreenUtils/clear color))
