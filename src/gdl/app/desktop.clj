(ns gdl.app.desktop
  (:require clojure.edn
            clojure.graphics.camera
            clojure.graphics.pixmap
            clojure.graphics.shape-drawer
            clojure.graphics.texture
            clojure.graphics.2d.texture-region
            clojure.java.io
            gdl.app
            gdl.context
            gdl.input
            gdl.graphics.camera
            gdl.graphics.color
            gdl.platform.libgdx
            gdl.scene2d.actor
            gdl.scene2d.group
            gdl.ui
            gdl.utils
            cdq.assets
            cdq.create
            cdq.db
            cdq.entity
            cdq.entity.state
            cdq.graphics
            cdq.graphics.animation
            cdq.graphics.default-font
            cdq.graphics.shape-drawer
            cdq.graphics.tiled-map
            cdq.graphics.tiled-map-renderer
            cdq.graphics.ui-viewport
            cdq.graphics.world-unit-scale
            cdq.graphics.world-viewport
            cdq.level
            cdq.schema
            cdq.ui.player-message
            cdq.ui.actionbar
            cdq.ui.dev-menu
            cdq.ui.hp-mana-bar
            cdq.ui.windows
            cdq.ui.entity-info-window
            cdq.ui.player-state
            cdq.ui.player-message
            cdq.time
            cdq.malli))

(defrecord Cursors []
  gdl.utils/Disposable
  (dispose [this]
    (run! gdl.utils/dispose (vals this))))

(defmethod cdq.schema/malli-form :s/val-max [_ _schemas] cdq.malli/val-max-schema)
(defmethod cdq.schema/malli-form :s/number  [_ _schemas] cdq.malli/number-schema)
(defmethod cdq.schema/malli-form :s/nat-int [_ _schemas] cdq.malli/nat-int-schema)
(defmethod cdq.schema/malli-form :s/int     [_ _schemas] cdq.malli/int-schema)
(defmethod cdq.schema/malli-form :s/pos     [_ _schemas] cdq.malli/pos-schema)
(defmethod cdq.schema/malli-form :s/pos-int [_ _schemas] cdq.malli/pos-int-schema)

(gdl.utils/defcomponent :s/sound
  (cdq.schema/malli-form [_ _schemas]
    cdq.malli/string-schema)

  (cdq.db/edn->value [_ sound-name _db c]
    (gdl.context/get-sound c sound-name)))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (gdl.context/from-sprite-sheet c
                                     (gdl.context/sprite-sheet c file tilew tileh)
                                     [(int (/ sprite-x tilew))
                                      (int (/ sprite-y tileh))]))
    (gdl.context/sprite c file)))

(gdl.utils/defcomponent :s/image
  (cdq.schema/malli-form  [_ _schemas]
    cdq.malli/image-schema)

  (cdq.db/edn->value [_ edn _db c]
    (edn->sprite c edn)))

(gdl.utils/defcomponent :s/animation
  (cdq.schema/malli-form [_ _schemas]
    cdq.malli/animation-schema)

  (cdq.db/edn->value [_ {:keys [frames frame-duration looping?]} _db c]
    (cdq.graphics.animation/create (map #(edn->sprite c %) frames)
                                   :frame-duration frame-duration
                                   :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(gdl.utils/defcomponent :s/one-to-one
  (cdq.schema/malli-form [[_ property-type] _schemas]
    (cdq.malli/qualified-keyword-schema (type->id-namespace property-type)))
  (cdq.db/edn->value [_ property-id db c]
    (gdl.context/build c property-id)))

(gdl.utils/defcomponent :s/one-to-many
  (cdq.schema/malli-form [[_ property-type] _schemas]
    (cdq.malli/set-schema (cdq.malli/qualified-keyword-schema (type->id-namespace property-type))))
  (cdq.db/edn->value [_ property-ids db c]
    (set (map #(gdl.context/build c %) property-ids))))

(defn- map-form [ks schemas]
  (cdq.malli/map-schema ks (fn [k]
                             (cdq.schema/malli-form (cdq.schema/schema-of schemas k)
                                                    schemas))))

(defmethod cdq.schema/malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod cdq.schema/malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod cdq.schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (cdq.schema/malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
                         schemas))

(defn -main []
  (let [render-fns [(fn [{:keys [gdl.graphics/world-viewport
                                 cdq.context/player-eid]
                          :as context}]
                      {:pre [world-viewport
                             player-eid]}
                      (gdl.graphics.camera/set-position! (:camera world-viewport)
                                                         (:position @player-eid))
                      context)
                    (fn [context]
                      (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
                      context)
                    (fn [{:keys [gdl.graphics/world-viewport
                                 cdq.context/tiled-map
                                 cdq.context/raycaster
                                 cdq.context/explored-tile-corners]
                          :as context}]
                      (gdl.context/draw-tiled-map context
                                                  tiled-map
                                                  (cdq.graphics.tiled-map/tile-color-setter raycaster
                                                                                            explored-tile-corners
                                                                                            (gdl.graphics.camera/position (:camera world-viewport))))
                      context)
                    (fn [context]
                      (let [render-fns [cdq.graphics/render-before-entities
                                        cdq.graphics/render-entities
                                        cdq.graphics/render-after-entities]]
                        (gdl.context/draw-on-world-view context
                                                        (fn [context]
                                                          (doseq [f render-fns]
                                                            (f context)))))
                      context)
                    (fn [{:keys [gdl.context/stage] :as context}]
                      (gdl.ui/draw stage (assoc context :gdl.context/unit-scale 1))
                      context)
                    (fn [context]
                      (gdl.ui/act (:gdl.context/stage context) context)
                      context)
                    (fn [{:keys [cdq.context/player-eid] :as c}]
                      (cdq.entity.state/manual-tick (cdq.entity/state-obj @player-eid) c)
                      c)
                    (fn [{:keys [cdq.context/mouseover-eid
                                 cdq.context/player-eid] :as c}]
                      (let [new-eid (if (gdl.context/mouse-on-actor? c)
                                      nil
                                      (let [player @player-eid
                                            hits (remove #(= (:z-order @%) :z-order/effect)
                                                         (cdq.context/point->entities c (gdl.context/world-mouse-position c)))]
                                        (->> cdq.context/render-z-order
                                             (gdl.utils/sort-by-order hits #(:z-order @%))
                                             reverse
                                             (filter #(cdq.context/line-of-sight? c player @%))
                                             first)))]
                        (when mouseover-eid
                          (swap! mouseover-eid dissoc :entity/mouseover?))
                        (when new-eid
                          (swap! new-eid assoc :entity/mouseover? true))
                        (assoc c :cdq.context/mouseover-eid new-eid)))
                    (fn [{:keys [cdq.context/player-eid
                                 error ; FIXME ! not `::` keys so broken !
                                 ] :as c}]
                      (let [pausing? true]
                        (assoc c :cdq.context/paused? (or error
                                                          (and pausing?
                                                               (cdq.entity.state/pause-game? (cdq.entity/state-obj @player-eid))
                                                               (not (or (gdl.input/key-just-pressed? :p)
                                                                        (gdl.input/key-pressed? :space))))))))
                    (fn [c]
                      (if (:cdq.context/paused? c)
                        c
                        (-> c
                            cdq.context/update-time
                            cdq.context/tick-potential-fields
                            cdq.context/tick-entities)))
                    (fn [c]
                      ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                      (doseq [eid (filter (comp :entity/destroyed? deref)
                                          (cdq.context/all-entities c))]
                        (cdq.context/remove-entity c eid)
                        (doseq [component @eid]
                          (cdq.context/destroy! component eid c)))
                      c)
                    (fn [{:keys [gdl.graphics/world-viewport]
                          :as context}]
                      (let [camera (:camera world-viewport)
                            zoom-speed 0.025]
                        (when (gdl.input/key-pressed? :minus)  (gdl.graphics.camera/inc-zoom camera    zoom-speed))
                        (when (gdl.input/key-pressed? :equals) (gdl.graphics.camera/inc-zoom camera (- zoom-speed))))
                      context)
                    (fn [c]
                      (let [window-hotkeys {:inventory-window   :i
                                            :entity-info-window :e}]
                        (doseq [window-id [:inventory-window
                                           :entity-info-window]
                                :when (gdl.input/key-just-pressed? (get window-hotkeys window-id))]
                          (gdl.scene2d.actor/toggle-visible! (get (:windows (:gdl.context/stage c)) window-id))))
                      (when (gdl.input/key-just-pressed? :escape)
                        (let [windows (gdl.scene2d.group/children (:windows (:gdl.context/stage c)))]
                          (when (some gdl.scene2d.actor/visible? windows)
                            (run! #(gdl.scene2d.actor/set-visible % false) windows))))
                      c)]
        create-fns [[:gdl/db (fn [_context _config]
                               (let [properties-file (clojure.java.io/resource "properties.edn")
                                     schemas (-> "schema.edn" clojure.java.io/resource slurp clojure.edn/read-string)
                                     properties (-> properties-file slurp clojure.edn/read-string)]
                                 (assert (or (empty? properties)
                                             (apply distinct? (map :property/id properties))))
                                 (run! (partial cdq.schema/validate! schemas) properties)
                                 {:db/data (zipmap (map :property/id properties) properties)
                                  :db/properties-file properties-file
                                  :db/schemas schemas}))]
                    [:gdl/assets [cdq.assets/create {:folder "resources/"
                                                     :type-exts {:sound   #{"wav"}
                                                                 :texture #{"png" "bmp"}}}]]
                    [:gdl.graphics/batch (fn [_context _config]
                                           (com.badlogic.gdx.graphics.g2d.SpriteBatch.))]
                    [:gdl.graphics/shape-drawer-texture (fn [_context _config]
                                                          (let [pixmap (doto (clojure.graphics.pixmap/create 1 1 clojure.graphics.pixmap/format-RGBA8888)
                                                                         (clojure.graphics.pixmap/set-color gdl.graphics.color/white)
                                                                         (clojure.graphics.pixmap/draw-pixel 0 0))
                                                                texture (clojure.graphics.texture/create pixmap)]
                                                            (gdl.utils/dispose pixmap)
                                                            texture))]
                    [:gdl.graphics/shape-drawer (fn [{:keys [gdl.graphics/batch
                                                             gdl.graphics/shape-drawer-texture]} _config]
                                                  (space.earlygrey.shapedrawer.ShapeDrawer. batch
                                                                                            (clojure.graphics.2d.texture-region/create shape-drawer-texture 1 0 1 1)))]
                    [:gdl.graphics/cursors (fn [_context _config]
                                             (map->Cursors
                                              (gdl.utils/mapvals
                                               (fn [[file [hotspot-x hotspot-y]]]
                                                 (let [pixmap (clojure.graphics.pixmap/create (.internal com.badlogic.gdx.Gdx/files (str "cursors/" file ".png")))
                                                       cursor (clojure.graphics/new-cursor pixmap hotspot-x hotspot-y)]
                                                   (gdl.utils/dispose pixmap)
                                                   cursor))
                                               {:cursors/bag                   ["bag001"       [0   0]]
                                                :cursors/black-x               ["black_x"      [0   0]]
                                                :cursors/default               ["default"      [0   0]]
                                                :cursors/denied                ["denied"       [16 16]]
                                                :cursors/hand-before-grab      ["hand004"      [4  16]]
                                                :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                                                :cursors/hand-grab             ["hand003"      [4  16]]
                                                :cursors/move-window           ["move002"      [16 16]]
                                                :cursors/no-skill-selected     ["denied003"    [0   0]]
                                                :cursors/over-button           ["hand002"      [0   0]]
                                                :cursors/sandclock             ["sandclock"    [16 16]]
                                                :cursors/skill-not-usable      ["x007"         [0   0]]
                                                :cursors/use-skill             ["pointer004"   [0   0]]
                                                :cursors/walking               ["walking"      [16 16]]})))]
                    [:gdl.graphics/default-font [cdq.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                                   :size 16
                                                                                   :quality-scaling 2}]]
                    [:gdl.graphics/world-unit-scale [cdq.graphics.world-unit-scale/create 48]]
                    [:gdl.graphics/tiled-map-renderer [cdq.graphics.tiled-map-renderer/create]]
                    [:gdl.graphics/ui-viewport [cdq.graphics.ui-viewport/create {:width 1440 :height 900}]]
                    [:gdl.graphics/world-viewport [cdq.graphics.world-viewport/create {:width 1440 :height 900}]]
                    [:gdl.context/stage [gdl.ui/setup-stage! {:skin-scale :x1
                                                              :actors [[cdq.ui.dev-menu/create]
                                                                       [cdq.ui.actionbar/create]
                                                                       [cdq.ui.hp-mana-bar/create]
                                                                       [cdq.ui.windows/create [cdq.ui.entity-info-window/create
                                                                                               cdq.widgets.inventory/create]]
                                                                       [cdq.ui.player-state/create]
                                                                       [cdq.ui.player-message/actor]]}]]
                    [:gdl.context/elapsed-time [cdq.time/create]]
                    [:cdq.context/player-message [cdq.ui.player-message/create* {:duration-seconds 1.5}]]
                    [:cdq.context/level [cdq.level/create :worlds/uf-caves]]
                    [:cdq.context/error [cdq.create/error*]]
                    [:cdq.context/tiled-map [cdq.create/tiled-map*]]
                    [:cdq.context/explored-tile-corners [cdq.create/explored-tile-corners*]]
                    [:cdq.context/grid [cdq.create/grid*]]
                    [:cdq.context/raycaster [cdq.create/create-raycaster]]
                    [:cdq.context/content-grid [cdq.create/content-grid* {:cell-size 16}]]
                    [:cdq.context/entity-ids [cdq.create/entity-ids*]]
                    [:cdq.context/factions-iterations [cdq.create/factions-iterations* {:good 15 :evil 5}]]
                    [:cdq.context/player-eid [cdq.create/player-eid*]]
                    [:cdq.context/enemies [cdq.create/spawn-enemies!]]]]
    (.setIconImage (java.awt.Taskbar/getTaskbar)
                   (.getImage (java.awt.Toolkit/getDefaultToolkit)
                              (clojure.java.io/resource "moon.png")))
    (when com.badlogic.gdx.utils.SharedLibraryLoader/isMac
      (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.
     (proxy [com.badlogic.gdx.ApplicationAdapter] []
       (create []
         (reset! gdl.app/state
                 (reduce (fn [context [k component]]
                           (let [f (if (vector? component)
                                     (component 0)
                                     component)
                                 params (if (and (vector? component) (= (count component) 2))
                                          (component 1)
                                          nil)]
                             (assoc context k (f context params))))
                         {}
                         create-fns)))

       (dispose []
         (doseq [[k value] @gdl.app/state
                 :when (gdl.utils/disposable? value)]
           ;(println "Disposing " k " - " value)
           (gdl.utils/dispose value)))

       (render []
         (swap! gdl.app/state (fn [context]
                                (reduce (fn [context f]
                                          (f context))
                                        context
                                        render-fns))))

       (resize [width height]
         (let [context @gdl.app/state]
           (com.badlogic.gdx.utils.viewport.Viewport/.update (:gdl.graphics/ui-viewport    context) width height true)
           (com.badlogic.gdx.utils.viewport.Viewport/.update (:gdl.graphics/world-viewport context) width height false))))
     (doto (com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.)
       (.setTitle "Cyber Dungeon Quest")
       (.setWindowedMode 1440 900)
       (.setForegroundFPS 60)))))
