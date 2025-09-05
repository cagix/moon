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
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [cdq.ui.ctx-stage :as ctx-stage]
            [clojure.vis-ui :as vis-ui]
            [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]))

(defn- load-cursors [files graphics cursors cursor-path-format]
  (update-vals cursors
               (fn [[file [hotspot-x hotspot-y]]]
                 (let [pixmap (pixmap/create (files/internal files (format cursor-path-format file)))
                       cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                   (.dispose pixmap)
                   cursor))))

(defn- assoc-graphics [ctx]
  (assoc ctx :ctx/graphics (gdx/graphics)))

(defn- assoc-textures [ctx]
  (assoc ctx :ctx/textures (cdq.textures-impl/create (gdx/files))))

(defn- assoc-audio
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/audio (audio/create (gdx/audio) (gdx/files) (:audio config))))

(defn- assoc-cursors
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/cursors (load-cursors (gdx/files) (gdx/graphics) (:cursors config) (:cursor-path-format config))))

(defn assoc-db
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/db (db/create (:db config))))

(defn- assoc-sprite-batch [ctx]
  (assoc ctx :ctx/batch (sprite-batch/create)))

(defn assoc-world-unit-scale [ctx]
  (assoc ctx :ctx/world-unit-scale (float (/ (:tile-size (:ctx/config ctx))))))

(defn- assoc-shape-drawer-texture [ctx]
  (assoc ctx :ctx/shape-drawer-texture (let [pixmap (doto (pixmap/create)
                                                      (pixmap/set-color! color/white)
                                                      (pixmap/draw-pixel! 0 0))
                                             texture (texture/create pixmap)]
                                         (pixmap/dispose! pixmap)
                                         texture)))

(defn assoc-shape-drawer
  [{:keys [ctx/shape-drawer-texture
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))))

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
  (assoc ctx :ctx/input (gdx/input)))

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
         (freetype/generate-font (files/internal (gdx/files) (:file (:default-font config)))
                                 (:params (:default-font config)))))

(defn assoc-unit-scale [ctx]
  (assoc ctx :ctx/unit-scale (atom 1)))

(defn do! [config]
  (colors/put! (:colors config))
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
