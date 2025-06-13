; TODO
; * ->Color ignored with lein codox (thinks its a defrecord constructor)
(ns clojure.gdx
  (:require [clojure.string :as str])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color
                                      Colors
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

(defn- safe-get-option [mapping k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))

(let [mapping {:black       Color/BLACK
               :blue        Color/BLUE
               :brown       Color/BROWN
               :chartreuse  Color/CHARTREUSE
               :clear       Color/CLEAR
               :clear-white Color/CLEAR_WHITE
               :coral       Color/CORAL
               :cyan        Color/CYAN
               :dark-gray   Color/DARK_GRAY
               :firebrick   Color/FIREBRICK
               :forest      Color/FOREST
               :gold        Color/GOLD
               :goldenrod   Color/GOLDENROD
               :gray        Color/GRAY
               :green       Color/GREEN
               :light-gray  Color/LIGHT_GRAY
               :lime        Color/LIME
               :magenta     Color/MAGENTA
               :maroon      Color/MAROON
               :navy        Color/NAVY
               :olive       Color/OLIVE
               :orange      Color/ORANGE
               :pink        Color/PINK
               :purple      Color/PURPLE
               :red         Color/RED
               :royal       Color/ROYAL
               :salmon      Color/SALMON
               :scarlet     Color/SCARLET
               :sky         Color/SKY
               :slate       Color/SLATE
               :tan         Color/TAN
               :teal        Color/TEAL
               :violet      Color/VIOLET
               :white       Color/WHITE
               :yellow      Color/YELLOW}]
  (defn k->Color [k]
    (safe-get-option mapping k)))

(defn ->Color ^Color [c]
  (cond (keyword? c) (k->Color c)
        (vector?  c) (let [[r g b a] c]
                       (Color. r g b a))
        :else (throw (ex-info "Cannot understand color" c))))

(defn ->float-bits [[r g b a]]
  (Color/toFloatBits (float r)
                     (float g)
                     (float b)
                     (float a)))

(defn def-colors [colors]
  (doseq [[name color-params] colors]
    (Colors/put name (->Color color-params))))

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
                 (.setColor (->Color :white))
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
  (.setColor batch (->Color :white))
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
  (ScreenUtils/clear (->Color color)))

(defn vector3->clj-vec [^Vector3 v3]
  [(.x v3)
   (.y v3)
   (.z v3)])
