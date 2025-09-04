(ns cdq.graphics-impl
  (:require [cdq.gdx.graphics.camera :as camera]
            [cdq.gdx.graphics.shape-drawer :as sd]
            [cdq.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [cdq.gdx.graphics.viewport :as viewport]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Files)
           (com.badlogic.gdx.graphics Color
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(comment
 Nearest ; Fetch the nearest texel that best maps to the pixel on screen.
Linear ; Fetch four nearest texels that best maps to the pixel on screen.
MipMap ; @see TextureFilter#MipMapLinearLinear
MipMapNearestNearest ; Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a nearest filter.
MipMapLinearNearest ; Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a linear filter.
MipMapNearestLinear ; Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images, combining them to the final output pixel.
MipMapLinearLinear ; Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two images, combining them to the final output pixel.
 )

(let [mapping {:linear Texture$TextureFilter/Linear}]
  (defn texture-filter-k->value [k]
    (when-not (contains? mapping k)
      (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys mapping))))))
    (k mapping)))

(defn- bitmap-font-configure! [^BitmapFont font {:keys [scale enable-markup? use-integer-positions?]}]
  (.setScale (.getData font) scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)

(defn- create-font-params [{:keys [size
                                   min-filter
                                   mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn- generate-font [file-handle {:keys [size
                                          quality-scaling
                                          enable-markup?
                                          use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator
                            (create-font-params {:size (* size quality-scaling)
                                                 ; :texture-filter/linear because scaling to world-units
                                                 :min-filter (texture-filter-k->value :linear)
                                                 :mag-filter (texture-filter-k->value :linear)}))]
    (bitmap-font-configure! font {:scale (/ quality-scaling)
                                  :enable-markup? enable-markup?
                                  :use-integer-positions? use-integer-positions?})))

(q/defrecord Graphics [g/batch
                       g/default-font
                       g/shape-drawer-texture
                       g/shape-drawer
                       g/tiled-map-renderer
                       g/ui-viewport
                       g/unit-scale
                       g/world-unit-scale
                       g/world-viewport])

(defn create!
  [{:keys [files]}
   {:keys [default-font
           tile-size
           ui-viewport
           world-viewport]}]
  (let [batch (SpriteBatch.)
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor Color/WHITE)
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
                               texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (viewport/fit (:width  ui-viewport)
                                  (:height ui-viewport)
                                  (camera/orthographic))]
    (map->Graphics
     {:default-font (when default-font
                      (generate-font (Files/.internal files (:file default-font))
                                     (:params default-font)))
      :world-unit-scale world-unit-scale
      :ui-viewport ui-viewport
      :world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                            world-height (* (:height world-viewport) world-unit-scale)]
                        (viewport/fit world-width
                                      world-height
                                      (camera/orthographic :y-down? false
                                                           :world-width world-width
                                                           :world-height world-height)))
      :batch batch
      :unit-scale (atom 1)
      :shape-drawer-texture shape-drawer-texture
      :shape-drawer (sd/create batch (TextureRegion. shape-drawer-texture 1 0 1 1))
      :tiled-map-renderer (tm-renderer/create world-unit-scale batch)})))
