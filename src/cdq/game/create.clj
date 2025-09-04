(ns cdq.game.create
  (:require [cdq.audio :as audio]
            [cdq.input :as input]
            [cdq.db-impl :as db]
            [cdq.textures-impl]
            [cdq.gdx.graphics :as graphics]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.viewport :as viewport]
            [cdq.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [cdq.gdx.graphics.shape-drawer :as sd]
            [cdq.gdx.ui :as ui]
            [cdq.malli :as m]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Files)
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

(q/defrecord Context [ctx/schema
                      ctx/config
                      ctx/cursors
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/mouseover-eid
                      ctx/player-eid
                      ctx/textures
                      ctx/tiled-map
                      ctx/tiled-map-renderer
                      ctx/gdx-graphics
                      ctx/ui-viewport
                      ctx/world
                      ctx/world-viewport
                      ctx/batch
                      ctx/default-font
                      ctx/shape-drawer-texture
                      ctx/shape-drawer
                      ctx/paused?
                      ctx/unit-scale
                      ctx/world-unit-scale])

; => this has to be pipelined
; and graphics/world abstractions are questionable
; and maybe ctx/gdx ?!
; use at handle draws ctx and world tick etc
; do select-keys so I know what is only used ?
; but nopt even necessary

; 1. gdx-graphics -> graphics
; 2. handle-draws! -> cdq.ctx (similar to txs)

(defn do! [gdx config]
  (doseq [[name color-params] (:colors config)]
    (Colors/put name (color/->obj color-params)))
  (ui/load! (::stage config))
  (let [input (:input gdx)
        world-unit-scale (float (/ (:tile-size config)))
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor Color/WHITE)
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
                               texture)
        ui-viewport (viewport/fit (:width  (:ui-viewport config))
                                  (:height (:ui-viewport config))
                                  (camera/orthographic))
        world-viewport (let [world-width  (* (:width  (:world-viewport config)) world-unit-scale)
                             world-height (* (:height (:world-viewport config)) world-unit-scale)]
                         (viewport/fit world-width
                                       world-height
                                       (camera/orthographic :y-down? false
                                                            :world-width world-width
                                                            :world-height world-height)))
        batch (SpriteBatch.)
        stage (ui/stage ui-viewport batch)]
    (input/set-processor! input stage)
    (-> (map->Context {:schema (m/schema [:map {:closed true}
                                          [:ctx/schema :some]
                                          [:ctx/config :some]
                                          [:ctx/cursors :some]
                                          [:ctx/input :some]
                                          [:ctx/db :some]
                                          [:ctx/audio :some]
                                          [:ctx/stage :some]
                                          [:ctx/mouseover-eid :any]
                                          [:ctx/player-eid :some]
                                          [:ctx/textures :some]
                                          [:ctx/gdx-graphics :some]
                                          [:ctx/tiled-map :some]
                                          [:ctx/tiled-map-renderer :some]
                                          [:ctx/ui-viewport :some]
                                          [:ctx/world :some]
                                          [:ctx/world-viewport :some]
                                          [:ctx/batch :some]
                                          [:ctx/paused? :any]
                                          [:ctx/default-font :some]
                                          [:ctx/shape-drawer-texture :some]
                                          [:ctx/shape-drawer :some]
                                          [:ctx/unit-scale :some]
                                          [:ctx/world-unit-scale :some]])
                       :gdx-graphics (:graphics gdx)
                       :textures (cdq.textures-impl/create (:files gdx))
                       :audio (audio/create gdx (::audio config))
                       :config config
                       :cursors (load-cursors
                                 (:files gdx)
                                 (:graphics gdx)
                                 (:cursors config)
                                 (:cursor-path-format config))
                       :db (db/create (::db config))
                       :ui-viewport ui-viewport
                       :input input
                       :stage stage
                       :tiled-map-renderer (tm-renderer/create world-unit-scale batch)
                       :world-viewport world-viewport
                       :default-font (generate-font (Files/.internal (:files gdx) (:file (:default-font config)))
                                                    (:params (:default-font config)))
                       :world-unit-scale world-unit-scale
                       :batch batch
                       :unit-scale (atom 1)
                       :shape-drawer-texture shape-drawer-texture
                       :shape-drawer (sd/create batch (TextureRegion. shape-drawer-texture 1 0 1 1))})
        ((requiring-resolve (:reset-game-state! config)) (::starting-level config))
        (assoc :ctx/mouseover-eid nil
               :ctx/paused? nil))))
