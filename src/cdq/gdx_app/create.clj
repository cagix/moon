(ns cdq.gdx-app.create
  (:require [cdq.audio :as audio]
            [cdq.input :as input]
            [cdq.db-impl :as db]
            [cdq.textures-impl]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.color :as color]
            [cdq.graphics.viewport :as viewport]
            [cdq.graphics.tiled-map-renderer :as tm-renderer]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.ui :as ui]
            [cdq.malli :as m]
            [qrecord.core :as q])
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

(q/defrecord Context [ctx/active-entities
                      ctx/audio
                      ctx/batch
                      ctx/config
                      ctx/content-grid
                      ctx/cursors
                      ctx/db
                      ctx/default-font
                      ctx/delta-time
                      ctx/elapsed-time
                      ctx/entity-ids
                      ctx/explored-tile-corners
                      ctx/factions-iterations
                      ctx/graphics
                      ctx/grid
                      ctx/id-counter
                      ctx/input
                      ctx/max-delta
                      ctx/max-speed
                      ctx/minimum-size
                      ctx/mouseover-eid
                      ctx/paused?
                      ctx/player-eid
                      ctx/potential-field-cache
                      ctx/raycaster
                      ctx/render-z-order
                      ctx/schema
                      ctx/shape-drawer
                      ctx/shape-drawer-texture
                      ctx/stage
                      ctx/textures
                      ctx/tiled-map
                      ctx/tiled-map-renderer
                      ctx/ui-viewport
                      ctx/unit-scale
                      ctx/world-unit-scale
                      ctx/world-viewport
                      ctx/z-orders])

(defn do! [config]
  (doseq [[name color-params] (:colors config)]
    (Colors/put name (color/->obj color-params)))
  (ui/load! (:stage config))
  (let [input Gdx/input
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
                                          [:ctx/active-entities :some]
                                          [:ctx/audio :some]
                                          [:ctx/batch :some]
                                          [:ctx/config :some]
                                          [:ctx/content-grid :some]
                                          [:ctx/cursors :some]
                                          [:ctx/db :some]
                                          [:ctx/default-font :some]
                                          [:ctx/delta-time :some]
                                          [:ctx/elapsed-time :some]
                                          [:ctx/entity-ids :some]
                                          [:ctx/explored-tile-corners :some]
                                          [:ctx/factions-iterations :some]
                                          [:ctx/graphics :some]
                                          [:ctx/grid :some]
                                          [:ctx/id-counter :some]
                                          [:ctx/input :some]
                                          [:ctx/max-delta :some]
                                          [:ctx/max-speed :some]
                                          [:ctx/minimum-size :some]
                                          [:ctx/mouseover-eid :any]
                                          [:ctx/paused? :any]
                                          [:ctx/player-eid :some]
                                          [:ctx/potential-field-cache :some]
                                          [:ctx/raycaster :some]
                                          [:ctx/render-z-order :some]
                                          [:ctx/schema :some]
                                          [:ctx/shape-drawer :some]
                                          [:ctx/shape-drawer-texture :some]
                                          [:ctx/stage :some]
                                          [:ctx/textures :some]
                                          [:ctx/tiled-map :some]
                                          [:ctx/tiled-map-renderer :some]
                                          [:ctx/ui-viewport :some]
                                          [:ctx/unit-scale :some]
                                          [:ctx/world-unit-scale :some]
                                          [:ctx/world-viewport :some]
                                          [:ctx/z-orders :some]])
                       :graphics Gdx/graphics
                       :textures (cdq.textures-impl/create Gdx/files)
                       :audio (audio/create Gdx/audio Gdx/files (:audio config))
                       :config config
                       :cursors (load-cursors
                                 Gdx/files
                                 Gdx/graphics
                                 (:cursors config)
                                 (:cursor-path-format config))
                       :db (db/create (:db config))
                       :ui-viewport ui-viewport
                       :input input
                       :stage stage
                       :tiled-map-renderer (tm-renderer/create world-unit-scale batch)
                       :world-viewport world-viewport
                       :default-font (generate-font (Files/.internal Gdx/files (:file (:default-font config)))
                                                    (:params (:default-font config)))
                       :world-unit-scale world-unit-scale
                       :batch batch
                       :unit-scale (atom 1)
                       :shape-drawer-texture shape-drawer-texture
                       :shape-drawer (sd/create batch (TextureRegion. shape-drawer-texture 1 0 1 1))})
        ((requiring-resolve (:reset-game-state! config)) (:starting-level config))
        (assoc :ctx/mouseover-eid nil
               :ctx/paused? nil
               :ctx/delta-time 2
               :ctx/active-entities 1))))
