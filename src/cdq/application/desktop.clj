(ns cdq.application.desktop
  (:require [clojure.app :as app]
            [clojure.application]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start [{:keys [config
                     create
                     render
                     resize]}]
  (lwjgl/application (reify app/Listener
                       (create [_]
                         (clojure.application/create create))

                       (dispose [_]
                         (clojure.application/dispose))

                       (pause [_])

                       (render [_]
                         (clojure.application/render render))

                       (resize [_ width height]
                         (clojure.application/resize resize width height))

                       (resume [_]))
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
