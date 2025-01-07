(ns cdq.app.listener
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.file-handle :as fh]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.vis-ui :as vis-ui]
            [clojure.string :as str]
            [gdl.context :as gdl.context]
            [gdl.db :as db]
            [gdl.ui :as ui]
            [gdl.utils :refer [mapvals safe-merge tile->middle]]
            [anvil.level :refer [generate-level]]
            [cdq.context :refer [spawn-creature]]
            [cdq.context.stage-actors :as stage-actors]
            [cdq.context.explored-tile-corners :as explored-tile-corners]
            [cdq.context.content-grid :as content-grid]
            [cdq.context.grid :as grid]
            [cdq.context.raycaster :as raycaster]

            gdl.context
            gdl.graphics
            cdq.context
            cdq.graphics
            cdq.graphics.camera
            cdq.graphics.tiled-map)
  (:import (com.kotcrab.vis.ui.widget Tooltip)
           (gdl OrthogonalTiledMapRenderer)))

(defn- load-all [manager assets]
  (doseq [[file asset-type] assets]
    (assets/load manager file asset-type))
  (assets/finish-loading manager))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn- load-assets [context folder]
  (doto (gdx/asset-manager)
    (load-all (for [[asset-type exts] {:sound   #{"wav"}
                                       :texture #{"png" "bmp"}}
                    file (map #(str/replace-first % folder "")
                              (recursively-search (gdx/internal-file context folder)
                                                  exts))]
                [file asset-type]))))

(defn- create-cursors [context cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (gdx/pixmap (gdx/internal-file context (str "cursors/" file ".png")))
                   cursor (gdx/cursor context pixmap hotspot-x hotspot-y)]
               (gdx/dispose pixmap)
               cursor))
           cursors))

(defn- white-pixel-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color gdx/white)
                 (gdx/draw-pixel 0 0))
        texture (gdx/texture pixmap)]
    (gdx/dispose pixmap)
    texture))

(defn- cached-tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- load-vis-ui! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (vis-ui/loaded?)
    (vis-ui/dispose))
  (vis-ui/load skin-scale)
  (-> (vis-ui/skin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (println "World-viewport: " width ", " height ", world-unit-scale " world-unit-scale)
  (assert world-unit-scale)
  (let [camera (gdx/orthographic-camera)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (gdx/fit-viewport world-width world-height camera)))

; TODO this passing w. world props ...
; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- spawn-player-entity [context start-position]
  (spawn-creature context (player-entity-props start-position)))

(defn- spawn-enemies! [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle))))

; TODO this is just application-context & world/game context
; or its one and we implement on game restart
(defn create [config]
  (load-vis-ui! (:vis-ui config))
  (let [context (gdx/context)
        batch (gdx/sprite-batch)
        sd-texture (white-pixel-texture)
        ui-viewport (gdx/fit-viewport (:width  (:ui-viewport config))
                                      (:height (:ui-viewport config))
                                      (gdx/orthographic-camera))
        world-unit-scale (float (/ (:tile-size config)))
        stage (ui/stage ui-viewport batch nil)
        _ (gdx/set-input-processor context stage)
        context (safe-merge context
                            {:gdl.context/assets (load-assets context (:assets config))
                             :gdl.context/batch batch
                             :gdl.context/cursors (create-cursors context (:cursors config))
                             :gdl.context/db (db/create (:db config))
                             :gdl.context/default-font (freetype/generate-font (update (:default-font config) :file #(gdx/internal-file context %)))
                             :gdl.context/shape-drawer (sd/create batch (gdx/texture-region sd-texture 1 0 1 1))
                             :gdl.context/sd-texture sd-texture
                             :gdl.context/stage stage
                             :gdl.context/viewport ui-viewport
                             :gdl.context/world-viewport (world-viewport (:world-viewport config) world-unit-scale)
                             :gdl.context/world-unit-scale world-unit-scale
                             :gdl.context/tiled-map-renderer (cached-tiled-map-renderer batch world-unit-scale)
                             :gdl.context/elapsed-time 0
                             :cdq.context/player-message (atom (:player-message config))})]
    (gdl.context/reset-stage context
                             (stage-actors/create context)) ; TODO this is not part of context keys anymore, can't be dispatched ... on actors
    ; so it should be part of stage/ui then? for resett-ing later ?
    (let [level (generate-level context
                                (gdl.context/build context (:world-id config)))
          tiled-map (:tiled-map level)
          grid (grid/create tiled-map)
          context (safe-merge context
                              {:cdq.context/error nil
                               :cdq.context/level level
                               :cdq.context/tiled-map tiled-map
                               :cdq.context/grid grid
                               :cdq.context/explored-tile-corners (explored-tile-corners/create tiled-map)
                               :cdq.context/content-grid (content-grid/create tiled-map (:content-grid config))
                               :cdq.context/entity-ids (atom {})
                               :cdq.context/raycaster (raycaster/create grid)
                               :cdq.context/factions-iterations (:factions-iterations config)})
          context (assoc context :cdq.context/player-eid (spawn-player-entity context (:start-position level)))]
      (spawn-enemies! context tiled-map)
      context)))

(defn dispose [context]
  (vis-ui/dispose)
  ; TODO dispose :gdl.context/sd-texture
  (gdx/dispose (:gdl.context/assets context))
  (gdx/dispose (:gdl.context/batch  context))
  (run! gdx/dispose (vals (:gdl.context/cursors context)))
  (gdx/dispose (:gdl.context/default-font context))
  (gdx/dispose (:gdl.context/stage context))
  (gdx/dispose (:cdq.context/tiled-map context))  ; TODO ! this also if world restarts !!
  )

(defn resize [context width height]
  (gdx/resize (:gdl.context/viewport       context) width height :center-camera? true)
  (gdx/resize (:gdl.context/world-viewport context) width height :center-camera? false))

; TODO
; just split in 'draw', and 'update' ? => 2 namespaces that's it !
(defn render [context]
  (reduce (fn [context f]
            (f context))
          context
          [gdl.graphics/clear-screen
           cdq.graphics.camera/set-on-player-position
           cdq.graphics.tiled-map/render
           cdq.graphics/draw-world-view
           gdl.graphics/draw-stage

           ; updates
           gdl.context/update-stage
           cdq.context/handle-player-input
           cdq.context/update-mouseover-entity
           cdq.context/update-paused-state
           cdq.context/progress-time-if-not-paused
           cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed.
           gdl.context/check-camera-controls
           cdq.context/check-ui-key-listeners]))
