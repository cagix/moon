(ns gdl.graphics
  (:require [clojure.files :as files]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.utils.screen :as screen]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [clojure.graphics :as graphics]
            [gdl.utils :refer [dispose mapvals]]
            [gdl.graphics.color :as color]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.graphics Colors Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)

           ; TODO gdl.graphics.!
           (gdl OrthogonalTiledMapRenderer)

           ))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (assert world-unit-scale)
  (let [camera (orthographic-camera/create)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (fit-viewport/create world-width world-height camera)))

(defn- cached-tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. file)
        font (.generateFont generator (ttf-params size quality-scaling))]
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- create-cursors [{:keys [clojure/files
                               clojure/graphics]} cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (pixmap/create (files/internal files (str "cursors/" file ".png")))
                   cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
               (dispose pixmap)
               cursor))
           cursors))

(defrecord Graphics []
  gdl.utils/Disposable
  (dispose [this]
    ;(println "Disposing batch")
    (dispose (:batch this))
    ;(println "Disposing sd-texture")
    (dispose (:sd-texture this))
    ;(println "Disposing cursors")
    (run! dispose (vals (:cursors this)))
    ;(println "Disposing default-font")
    (dispose (:default-font this)))
  gdl.utils/Resizable
  (resize [this width height]
    ;(println "Resizing ui-viewport.")
    (viewport/resize (:ui-viewport    this) width height :center-camera? true)
    ;(println "Resizing world-viewport.")
    (viewport/resize (:world-viewport this) width height :center-camera? false)))

(defn create [{:keys [clojure/files] :as context} config]
  (let [batch (SpriteBatch.)
        sd-texture (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                                  (pixmap/set-color color/white)
                                  (pixmap/draw-pixel 0 0))
                         texture (texture/create pixmap)]
                     (dispose pixmap)
                     texture)
        world-unit-scale (float (/ (:tile-size config)))]
    (map->Graphics
     {:batch batch
      :sd (sd/create batch (texture-region/create sd-texture 1 0 1 1))
      :sd-texture sd-texture
      :cursors (create-cursors context (:cursors config))
      :default-font (generate-font (update (:default-font config) :file #(files/internal files %)))
      :world-unit-scale world-unit-scale
      :tiled-map-renderer (cached-tiled-map-renderer batch world-unit-scale)
      :ui-viewport (fit-viewport/create (:width  (:ui-viewport config))
                                        (:height (:ui-viewport config))
                                        (orthographic-camera/create))
      :world-viewport (world-viewport (:world-viewport config) world-unit-scale)
      })))

(defn clear-screen [context]
  (screen/clear color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)

(defn delta-time
  "The time span between the current frame and the last frame in seconds."
  [graphics]
  (com.badlogic.gdx.Graphics/.getDeltaTime graphics))

(defn frames-per-second
  "The average number of frames per second."
  [graphics]
  (com.badlogic.gdx.Graphics/.getFramesPerSecond graphics))

(defn def-color
  "A general purpose class containing named colors that can be changed at will. For example, the markup language defined by the BitmapFontCache class uses this class to retrieve colors and the user can define his own colors.

  Convenience method to add a color with its name. The invocation of this method is equivalent to the expression Colors.getColors().put(name, color)

  Parameters:
  name - the name of the color
  color - the color
  Returns:
  the previous color associated with name, or null if there was no mapping for name ."
  [name-str color]
  (Colors/put name-str color))
