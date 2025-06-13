(ns clojure.gdx
  (:require [clojure.string :as str]
            [gdx.graphics.color :as color])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Colors
                                      OrthographicCamera
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d Batch
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.math Vector2
                                  Vector3)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport
                                            Viewport)))

(defn def-colors [colors]
  (doseq [[name color-params] colors]
    (Colors/put name (color/->obj color-params))))

(defn recursively-search [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn find-assets [^FileHandle folder extensions]
  (map #(str/replace-first % (str (.path folder) "/") "")
       (recursively-search folder extensions)))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor (color/->obj :white))
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn load-texture [^String path]
  (Texture. path))

(defn orthographic-camera
  ([]
   (OrthographicCamera.))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (OrthographicCamera.)
     (.setToOrtho y-down?
                  world-width
                  world-height))))

(defn draw-texture-region! [^Batch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn sprite-batch []
  (SpriteBatch.))

(defn fit-viewport [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :width  (.getWorldWidth  this)
        :height (.getWorldHeight this)
        :camera (.getCamera      this)))))

(defn clamp [value min max] (cond
   (< value min) min
   (> value max) max
   :else value))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn unproject [^Viewport viewport [x y]]
  (let [x (clamp x (.getLeftGutterWidth viewport) (.getRightGutterX    viewport))
        y (clamp y (.getTopGutterHeight viewport) (.getTopGutterY      viewport))]
    (let [vector2 (.unproject viewport (Vector2. x y))]
      [(.x vector2)
       (.y vector2)])))

(defn draw-on-viewport! [^Batch batch ^Viewport viewport f]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (.setColor batch (color/->obj :white))
  (.setProjectionMatrix batch (.combined (:camera viewport)))
  (.begin batch)
  (f)
  (.end batch))

(defn texture-region
  ([texture]
   (TextureRegion. texture))
  ([texture-region x y w h]
   (TextureRegion. texture-region
                   (int x)
                   (int y)
                   (int w)
                   (int h))))

(defn clear-screen! [color]
  (ScreenUtils/clear (color/->obj color)))

(defn vector3->clj-vec [^Vector3 v3]
  [(.x v3)
   (.y v3)
   (.z v3)])
