(ns cdq.gdx-app.create
  (:require [cdq.audio :as audio]
            [cdq.input :as input]
            [cdq.db-impl :as db]
            [cdq.textures-impl]
            [cdq.game-record :as game-record]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.viewport :as viewport]
            [cdq.graphics.tiled-map-renderer :as tm-renderer]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.ui.ctx-stage :as ctx-stage]
            [cdq.vis-ui :as vis-ui]
            [clojure.gdx.graphics.color :as color])
  (:import (com.badlogic.gdx Gdx
                             Files)
           (com.badlogic.gdx.graphics Color
                                      Colors
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

(defn- load-cursors [files graphics cursors cursor-path-format]
  (update-vals cursors
               (fn [[file [hotspot-x hotspot-y]]]
                 (let [pixmap (Pixmap. (Files/.internal files (format cursor-path-format file)))
                       cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                   (.dispose pixmap)
                   cursor))))

(defn- assoc-graphics [ctx]
  (assoc ctx :ctx/graphics Gdx/graphics))

(defn- assoc-textures [ctx]
  (assoc ctx :ctx/textures (cdq.textures-impl/create Gdx/files)))

(defn- assoc-audio
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/audio (audio/create Gdx/audio Gdx/files (:audio config))))

(defn- put-colors! [colors]
  (doseq [[name color-params] colors]
    (Colors/put name (color/->obj color-params))))

(defn- assoc-cursors
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/cursors (load-cursors Gdx/files Gdx/graphics (:cursors config) (:cursor-path-format config))))

(defn assoc-db
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/db (db/create (:db config))))

(defn- assoc-sprite-batch [ctx]
  (assoc ctx :ctx/batch (SpriteBatch.)))

(defn assoc-world-unit-scale [ctx]
  (assoc ctx :ctx/world-unit-scale (float (/ (:tile-size (:ctx/config ctx))))))

(defn- assoc-shape-drawer-texture [ctx]
  (assoc ctx :ctx/shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                                      (.setColor Color/WHITE)
                                                      (.drawPixel 0 0))
                                             texture (Texture. pixmap)]
                                         (.dispose pixmap)
                                         texture)))

(defn assoc-shape-drawer
  [{:keys [ctx/shape-drawer-texture
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/shape-drawer (sd/create batch (TextureRegion. shape-drawer-texture 1 0 1 1))))

(defn reset-stage-and-world-state!
  [{:keys [ctx/config]
    :as ctx}]
  ((requiring-resolve (:reset-game-state! config)) ctx (:starting-level config)))

(defn- assoc-frame-keys-for-schema [ctx]
  (assoc ctx :ctx/mouseover-eid nil
         :ctx/paused? nil
         :ctx/delta-time 2
         :ctx/active-entities 1))

(defn- set-stage-as-input-processor!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (input/set-processor! input stage)
  ctx)

(defn assoc-stage
  [{:keys [ctx/ui-viewport
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/stage (ctx-stage/create ui-viewport batch)))

(defn assoc-tiled-map-renderer
  [{:keys [ctx/world-unit-scale
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))

(defn assoc-input [ctx]
  (assoc ctx :ctx/input Gdx/input))

(defn assoc-ui-viewport [{:keys [ctx/config] :as ctx}]
  (assoc ctx :ctx/ui-viewport (viewport/fit (:width  (:ui-viewport config))
                                            (:height (:ui-viewport config))
                                            (camera/orthographic))))

(defn assoc-world-viewport
  [{:keys [ctx/config
           ctx/world-unit-scale]
    :as ctx}]
  (assoc ctx :ctx/world-viewport (let [world-width  (* (:width  (:world-viewport config)) world-unit-scale)
                                       world-height (* (:height (:world-viewport config)) world-unit-scale)]
                                   (viewport/fit world-width
                                                 world-height
                                                 (camera/orthographic :y-down? false
                                                                      :world-width world-width
                                                                      :world-height world-height)))))

(defn assoc-default-font
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/default-font
         (generate-font (Files/.internal Gdx/files (:file (:default-font config)))
                        (:params (:default-font config)))))

(defn assoc-unit-scale [ctx]
  (assoc ctx :ctx/unit-scale (atom 1)))

(defn do! [config]
  (put-colors! (:colors config))
  (vis-ui/load! (:stage config))
  (-> (game-record/create-with-schema)
      (assoc :ctx/config config)
      assoc-sprite-batch
      assoc-graphics
      assoc-textures
      assoc-audio
      assoc-cursors
      assoc-db
      assoc-world-unit-scale
      assoc-ui-viewport
      assoc-input
      assoc-stage
      set-stage-as-input-processor!
      assoc-tiled-map-renderer
      assoc-world-viewport
      assoc-default-font
      assoc-unit-scale
      assoc-shape-drawer-texture
      assoc-shape-drawer
      reset-stage-and-world-state! ; koennte man auch als assoc ausdruecken ...
      assoc-frame-keys-for-schema))
