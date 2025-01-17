(ns cdq.application.desktop
  (:require [clojure.application]
            [clojure.gdx.backends.lwjgl]))

(defn start [{:keys [config
                     create
                     render
                     resize]}]
  (clojure.gdx.backends.lwjgl/application
   (proxy [com.badlogic.gdx.ApplicationAdapter] []
     (create []
       (clojure.application/create create))

     (dispose []
       (clojure.application/dispose))

     (render []
       (clojure.application/render render))

     (resize [width height]
       (clojure.application/resize resize width height)))
   config))


; Badlogic => 'move to 'clojure.gdx' or whatever name it will be...
; * ApplicationAdapter
; * Color
; * SharedLibraryLoader
; * FitViewport
; * Viewport
; * Gdx
; * BitmapFont
; * Texture / TextureRegion
; * Camera OrthographicCamera
; * FileHandle
; * Pixmap
; * ScreenUtils
; * com.badlogic.gdx.graphics.g2d.SpriteBatch
; * com.badlogic.gdx.graphics.Colors/put
; * Input$Buttons / Input$Keys
; * Align
; com.badlogic.gdx.maps.tiled TmxMapLoader
; * math
; * sound
; * disposable
; * batch

; * com.badlogic.gdx.graphics.g2d.freetype

; * scene2d ...

; com.badlogic.gdx.assets AssetManager
