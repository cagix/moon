; you should not depend on implementation details ....


; TODO = only know about general components here, no mix anonymous fns with non-anonymous

; => every 'form' in its most minimal depednent place !!!


(ns cdq.application
  (:require cdq.graphics.shape-drawer
            clojure.app ; clean
            clojure.assets ; clean
            clojure.context ; lots of requires
            clojure.create ; different things
            clojure.db
            clojure.edn
            clojure.entity
            clojure.entity.state
            clojure.graphics
            clojure.graphics.animation
            clojure.graphics.camera
            clojure.graphics.color
            clojure.graphics.default-font
            clojure.graphics.pixmap
            clojure.graphics.shape-drawer
            clojure.graphics.texture
            clojure.graphics.tiled-map
            clojure.graphics.tiled-map-renderer
            clojure.graphics.ui-viewport
            clojure.graphics.world-unit-scale
            clojure.graphics.world-viewport
            clojure.input
            clojure.java.io
            clojure.level
            clojure.malli
            clojure.potential-fields
            clojure.platform.libgdx
            clojure.schema
            clojure.scene2d.actor
            clojure.scene2d.group
            clojure.ui
            clojure.ui.player-message
            clojure.ui.actionbar
            clojure.ui.dev-menu
            clojure.ui.hp-mana-bar
            clojure.ui.windows
            clojure.ui.entity-info-window
            clojure.ui.player-state
            clojure.ui.player-message
            clojure.utils
            clojure.world
            clojure.world.graphics))

(def ^:private pf-cache (atom nil))

(defn update-time [context]
  (let [delta-ms (min (clojure.graphics/delta-time) clojure.world/max-delta-time)]
    (-> context
        (update :clojure.context/elapsed-time + delta-ms)
        (assoc :clojure.context/delta-time delta-ms))))

(defn update-potential-fields [{:keys [clojure.context/factions-iterations
                                       clojure.context/grid] :as c}]
  (let [entities (clojure.world/active-entities c)]
    (doseq [[faction max-iterations] factions-iterations]
      (clojure.potential-fields/tick pf-cache
                                 grid
                                 faction
                                 entities
                                 max-iterations)))
  c)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn update-entities [c]
  (try
   (doseq [eid (clojure.world/active-entities c)]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (clojure.world/tick! [k v] eid c))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (clojure.context/error-window c t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  c)

(defrecord Cursors []
  clojure.utils/Disposable
  (dispose [this]
    (run! clojure.utils/dispose (vals this))))

(defmethod clojure.schema/malli-form :s/val-max [_ _schemas] clojure.malli/val-max-schema)
(defmethod clojure.schema/malli-form :s/number  [_ _schemas] clojure.malli/number-schema)
(defmethod clojure.schema/malli-form :s/nat-int [_ _schemas] clojure.malli/nat-int-schema)
(defmethod clojure.schema/malli-form :s/int     [_ _schemas] clojure.malli/int-schema)
(defmethod clojure.schema/malli-form :s/pos     [_ _schemas] clojure.malli/pos-schema)
(defmethod clojure.schema/malli-form :s/pos-int [_ _schemas] clojure.malli/pos-int-schema)

(clojure.utils/defcomponent :s/sound
  (clojure.schema/malli-form [_ _schemas]
    clojure.malli/string-schema)

  (clojure.db/edn->value [_ sound-name _db c]
    (clojure.context/get-sound c sound-name)))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (clojure.context/from-sprite-sheet c
                                     (clojure.context/sprite-sheet c file tilew tileh)
                                     [(int (/ sprite-x tilew))
                                      (int (/ sprite-y tileh))]))
    (clojure.context/sprite c file)))

(clojure.utils/defcomponent :s/image
  (clojure.schema/malli-form  [_ _schemas]
    clojure.malli/image-schema)

  (clojure.db/edn->value [_ edn _db c]
    (edn->sprite c edn)))

(clojure.utils/defcomponent :s/animation
  (clojure.schema/malli-form [_ _schemas]
    clojure.malli/animation-schema)

  (clojure.db/edn->value [_ {:keys [frames frame-duration looping?]} _db c]
    (clojure.graphics.animation/create (map #(edn->sprite c %) frames)
                                   :frame-duration frame-duration
                                   :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(clojure.utils/defcomponent :s/one-to-one
  (clojure.schema/malli-form [[_ property-type] _schemas]
    (clojure.malli/qualified-keyword-schema (type->id-namespace property-type)))
  (clojure.db/edn->value [_ property-id db c]
    (clojure.context/build c property-id)))

(clojure.utils/defcomponent :s/one-to-many
  (clojure.schema/malli-form [[_ property-type] _schemas]
    (clojure.malli/set-schema (clojure.malli/qualified-keyword-schema (type->id-namespace property-type))))
  (clojure.db/edn->value [_ property-ids db c]
    (set (map #(clojure.context/build c %) property-ids))))

(defn- map-form [ks schemas]
  (clojure.malli/map-schema ks (fn [k]
                             (clojure.schema/malli-form (clojure.schema/schema-of schemas k)
                                                    schemas))))

(defmethod clojure.schema/malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod clojure.schema/malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod clojure.schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (clojure.schema/malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
                         schemas))

(defn -main []
  (let [render-fns [(fn [{:keys [clojure.graphics/world-viewport
                                 clojure.context/player-eid]
                          :as context}]
                      {:pre [world-viewport
                             player-eid]}
                      (clojure.graphics.camera/set-position (:camera world-viewport)
                                                            (:position @player-eid))
                      context)
                    (fn [context]
                      (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
                      context)
                    (fn [{:keys [clojure.graphics/world-viewport
                                 clojure.context/tiled-map
                                 clojure.context/raycaster
                                 clojure.context/explored-tile-corners]
                          :as context}]
                      (clojure.context/draw-tiled-map context
                                                  tiled-map
                                                  (clojure.graphics.tiled-map/tile-color-setter raycaster
                                                                                            explored-tile-corners
                                                                                            (clojure.graphics.camera/position (:camera world-viewport))))
                      context)
                    (fn [context]
                      (let [render-fns [clojure.world.graphics/render-before-entities
                                        clojure.world.graphics/render-entities
                                        clojure.world.graphics/render-after-entities]]
                        (clojure.context/draw-on-world-view context
                                                        (fn [context]
                                                          (doseq [f render-fns]
                                                            (f context)))))
                      context)
                    (fn [{:keys [clojure.context/stage] :as context}]
                      (clojure.ui/draw stage (assoc context :clojure.context/unit-scale 1))
                      context)
                    (fn [context]
                      (clojure.ui/act (:clojure.context/stage context) context)
                      context)
                    (fn [{:keys [clojure.context/player-eid] :as c}]
                      (clojure.entity.state/manual-tick (clojure.entity/state-obj @player-eid) c)
                      c)
                    (fn [{:keys [clojure.context/mouseover-eid
                                 clojure.context/player-eid] :as c}]
                      (let [new-eid (if (clojure.context/mouse-on-actor? c)
                                      nil
                                      (let [player @player-eid
                                            hits (remove #(= (:z-order @%) :z-order/effect)
                                                         (clojure.world/point->entities c (clojure.context/world-mouse-position c)))]
                                        (->> clojure.world/render-z-order
                                             (clojure.utils/sort-by-order hits #(:z-order @%))
                                             reverse
                                             (filter #(clojure.world/line-of-sight? c player @%))
                                             first)))]
                        (when mouseover-eid
                          (swap! mouseover-eid dissoc :entity/mouseover?))
                        (when new-eid
                          (swap! new-eid assoc :entity/mouseover? true))
                        (assoc c :clojure.context/mouseover-eid new-eid)))
                    (fn [{:keys [clojure.context/player-eid
                                 error ; FIXME ! not `::` keys so broken !
                                 ] :as c}]
                      (let [pausing? true]
                        (assoc c :clojure.context/paused? (or error
                                                          (and pausing?
                                                               (clojure.entity.state/pause-game? (clojure.entity/state-obj @player-eid))
                                                               (not (or (clojure.input/key-just-pressed? :p)
                                                                        (clojure.input/key-pressed? :space))))))))
                    (fn [c]
                      (if (:clojure.context/paused? c)
                        c
                        (-> c
                            update-time
                            update-potential-fields
                            update-entities)))
                    (fn [c]
                      ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                      (doseq [eid (filter (comp :entity/destroyed? deref)
                                          (clojure.world/all-entities c))]
                        (clojure.world/remove-entity c eid)
                        (doseq [component @eid]
                          (clojure.world/destroy! component eid c)))
                      c)
                    (fn [{:keys [clojure.graphics/world-viewport]
                          :as context}]
                      (let [camera (:camera world-viewport)
                            zoom-speed 0.025]
                        (when (clojure.input/key-pressed? :minus)  (clojure.graphics.camera/inc-zoom camera    zoom-speed))
                        (when (clojure.input/key-pressed? :equals) (clojure.graphics.camera/inc-zoom camera (- zoom-speed))))
                      context)
                    (fn [c]
                      (let [window-hotkeys {:inventory-window   :i
                                            :entity-info-window :e}]
                        (doseq [window-id [:inventory-window
                                           :entity-info-window]
                                :when (clojure.input/key-just-pressed? (get window-hotkeys window-id))]
                          (clojure.scene2d.actor/toggle-visible! (get (:windows (:clojure.context/stage c)) window-id))))
                      (when (clojure.input/key-just-pressed? :escape)
                        (let [windows (clojure.scene2d.group/children (:windows (:clojure.context/stage c)))]
                          (when (some clojure.scene2d.actor/visible? windows)
                            (run! #(clojure.scene2d.actor/set-visible % false) windows))))
                      c)]
        create-fns [[:clojure/db (fn [_context _config]
                                   (let [properties-file (clojure.java.io/resource "properties.edn")
                                         schemas (-> "schema.edn" clojure.java.io/resource slurp clojure.edn/read-string)
                                         properties (-> properties-file slurp clojure.edn/read-string)]
                                     (assert (or (empty? properties)
                                                 (apply distinct? (map :property/id properties))))
                                     (run! (partial clojure.schema/validate! schemas) properties)
                                     {:db/data (zipmap (map :property/id properties) properties)
                                      :db/properties-file properties-file
                                      :db/schemas schemas}))]
                    [:clojure/assets clojure.assets/manager]
                    [:clojure.graphics/batch clojure.graphics/sprite-batch]
                    [:clojure.graphics/shape-drawer-texture (fn [_context _config]
                                                              (let [pixmap (doto (clojure.graphics.pixmap/create 1 1 clojure.graphics.pixmap/format-RGBA8888)
                                                                             (clojure.graphics.pixmap/set-color clojure.graphics.color/white)
                                                                             (clojure.graphics.pixmap/draw-pixel 0 0))
                                                                    texture (clojure.graphics.texture/create pixmap)]
                                                                (clojure.utils/dispose pixmap)
                                                                texture))]
                    [:clojure.graphics/shape-drawer cdq.graphics.shape-drawer/create]
                    [:clojure.graphics/cursors (fn [_context _config]
                                                 (map->Cursors
                                                  (clojure.utils/mapvals
                                                   (fn [[file [hotspot-x hotspot-y]]]
                                                     (let [pixmap (clojure.graphics.pixmap/create (.internal com.badlogic.gdx.Gdx/files (str "cursors/" file ".png")))
                                                           cursor (clojure.graphics/new-cursor pixmap hotspot-x hotspot-y)]
                                                       (clojure.utils/dispose pixmap)
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
                    [:clojure.graphics/default-font [clojure.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                                       :size 16
                                                                                       :quality-scaling 2}]]
                    [:clojure.graphics/world-unit-scale [clojure.graphics.world-unit-scale/create 48]]
                    [:clojure.graphics/tiled-map-renderer [clojure.graphics.tiled-map-renderer/create]]
                    [:clojure.graphics/ui-viewport [clojure.graphics.ui-viewport/create {:width 1440 :height 900}]]
                    [:clojure.graphics/world-viewport [clojure.graphics.world-viewport/create {:width 1440 :height 900}]]
                    [:clojure.context/stage [clojure.ui/setup-stage! {:skin-scale :x1
                                                                      :actors [[clojure.ui.dev-menu/create]
                                                                               [clojure.ui.actionbar/create]
                                                                               [clojure.ui.hp-mana-bar/create]
                                                                               [clojure.ui.windows/create [clojure.ui.entity-info-window/create
                                                                                                           clojure.widgets.inventory/create]]
                                                                               [clojure.ui.player-state/create]
                                                                               [clojure.ui.player-message/actor]]}]]
                    [:clojure.context/elapsed-time (fn [_context _config] 0)]
                    [:clojure.context/player-message [clojure.ui.player-message/create* {:duration-seconds 1.5}]]
                    [:clojure.context/level [clojure.level/create :worlds/uf-caves]]
                    [:clojure.context/error [clojure.create/error*]]
                    [:clojure.context/tiled-map [clojure.create/tiled-map*]]
                    [:clojure.context/explored-tile-corners [clojure.create/explored-tile-corners*]]
                    [:clojure.context/grid [clojure.create/grid*]]
                    [:clojure.context/raycaster [clojure.create/create-raycaster]]
                    [:clojure.context/content-grid [clojure.create/content-grid* {:cell-size 16}]]
                    [:clojure.context/entity-ids [clojure.create/entity-ids*]]
                    [:clojure.context/factions-iterations [clojure.create/factions-iterations* {:good 15 :evil 5}]]
                    [:clojure.context/player-eid [clojure.create/player-eid*]]
                    [:clojure.context/enemies [clojure.create/spawn-enemies!]]]]
    (.setIconImage (java.awt.Taskbar/getTaskbar)
                   (.getImage (java.awt.Toolkit/getDefaultToolkit)
                              (clojure.java.io/resource "moon.png")))
    (when com.badlogic.gdx.utils.SharedLibraryLoader/isMac
      (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.
     (proxy [com.badlogic.gdx.ApplicationAdapter] []
       (create []
         (reset! clojure.app/state
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
         (doseq [[k value] @clojure.app/state
                 :when (clojure.utils/disposable? value)]
           ;(println "Disposing " k " - " value)
           (clojure.utils/dispose value)))

       (render []
         (swap! clojure.app/state (fn [context]
                                (reduce (fn [context f]
                                          (f context))
                                        context
                                        render-fns))))

       (resize [width height]
         (let [context @clojure.app/state]
           (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/ui-viewport    context) width height true)
           (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/world-viewport context) width height false))))
     (doto (com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.)
       (.setTitle "Clojure")
       (.setWindowedMode 1440 900)
       (.setForegroundFPS 60)))))
