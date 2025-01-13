(ns clojure.app.desktop
  (:require clojure.assets
            clojure.edn
            clojure.graphics
            clojure.graphics.camera
            clojure.graphics.color
            clojure.graphics.pixmap
            clojure.graphics.shape-drawer
            clojure.graphics.texture
            clojure.graphics.tiled-map-renderer
            clojure.graphics.2d.texture-region
            clojure.java.io
            clojure.app
            clojure.context
            clojure.input
            clojure.interop
            clojure.platform.libgdx
            clojure.scene2d.actor
            clojure.scene2d.group
            clojure.ui
            clojure.utils
            cdq.create
            cdq.db
            cdq.entity
            cdq.entity.state
            cdq.graphics
            cdq.graphics.animation
            cdq.graphics.default-font
            cdq.graphics.tiled-map
            cdq.graphics.ui-viewport
            cdq.graphics.world-unit-scale
            cdq.graphics.world-viewport
            cdq.level
            cdq.schema
            cdq.potential-fields
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

(def ^:private pf-cache (atom nil))

(defn update-time [context]
  (let [delta-ms (min (clojure.graphics/delta-time) cdq.context/max-delta-time)]
    (-> context
        (update :clojure.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defn update-potential-fields [{:keys [cdq.context/factions-iterations
                                       cdq.context/grid] :as c}]
  (let [entities (cdq.context/active-entities c)]
    (doseq [[faction max-iterations] factions-iterations]
      (cdq.potential-fields/tick pf-cache
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
   (doseq [eid (cdq.context/active-entities c)]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (cdq.context/tick! [k v] eid c))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (clojure.context/error-window c t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  c)

(defn- munge-color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (clojure.interop/k->color c)
        (vector? c) (apply clojure.graphics.color/create c)
        :else (throw (ex-info "Cannot understand color" c))))

(let [set-color (fn [shape-drawer color]
                  (space.earlygrey.shapedrawer.ShapeDrawer/.setColor shape-drawer ^com.badlogic.gdx.graphics.Color (munge-color color)))]
  (extend-type space.earlygrey.shapedrawer.ShapeDrawer
    clojure.graphics.shape-drawer/ShapeDrawer
    (ellipse [this [x y] radius-x radius-y color]
      (set-color this color)
      (.ellipse this
                (float x)
                (float y)
                (float radius-x)
                (float radius-y)))

    (filled-ellipse [this [x y] radius-x radius-y color]
      (set-color this color)
      (.filledEllipse this
                      (float x)
                      (float y)
                      (float radius-x)
                      (float radius-y)))

    (circle [this [x y] radius color]
      (set-color this color)
      (.circle this
               (float x)
               (float y)
               (float radius)))

    (filled-circle [this [x y] radius color]
      (set-color this color)
      (.filledCircle this
                     (float x)
                     (float y)
                     (float radius)))

    (arc [this [center-x center-y] radius start-angle degree color]
      (set-color this color)
      (.arc this
            (float center-x)
            (float center-y)
            (float radius)
            (float (clojure.math.utils/degree->radians start-angle))
            (float (clojure.math.utils/degree->radians degree))))

    (sector [this [center-x center-y] radius start-angle degree color]
      (set-color this color)
      (.sector this
               (float center-x)
               (float center-y)
               (float radius)
               (float (clojure.math.utils/degree->radians start-angle))
               (float (clojure.math.utils/degree->radians degree))))

    (rectangle [this x y w h color]
      (set-color this color)
      (.rectangle this
                  (float x)
                  (float y)
                  (float w)
                  (float h)))

    (filled-rectangle [this x y w h color]
      (set-color this color)
      (.filledRectangle this
                        (float x)
                        (float y)
                        (float w)
                        (float h)))

    (line [this [sx sy] [ex ey] color]
      (set-color this color)
      (.line this
             (float sx)
             (float sy)
             (float ex)
             (float ey)))

    (with-line-width [this width draw-fn]
      (let [old-line-width (.getDefaultLineWidth this)]
        (.setDefaultLineWidth this (float (* width old-line-width)))
        (draw-fn)
        (.setDefaultLineWidth this (float old-line-width))))))

(defrecord Cursors []
  clojure.utils/Disposable
  (dispose [this]
    (run! clojure.utils/dispose (vals this))))

(defmethod cdq.schema/malli-form :s/val-max [_ _schemas] cdq.malli/val-max-schema)
(defmethod cdq.schema/malli-form :s/number  [_ _schemas] cdq.malli/number-schema)
(defmethod cdq.schema/malli-form :s/nat-int [_ _schemas] cdq.malli/nat-int-schema)
(defmethod cdq.schema/malli-form :s/int     [_ _schemas] cdq.malli/int-schema)
(defmethod cdq.schema/malli-form :s/pos     [_ _schemas] cdq.malli/pos-schema)
(defmethod cdq.schema/malli-form :s/pos-int [_ _schemas] cdq.malli/pos-int-schema)

(clojure.utils/defcomponent :s/sound
  (cdq.schema/malli-form [_ _schemas]
    cdq.malli/string-schema)

  (cdq.db/edn->value [_ sound-name _db c]
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
  (cdq.schema/malli-form  [_ _schemas]
    cdq.malli/image-schema)

  (cdq.db/edn->value [_ edn _db c]
    (edn->sprite c edn)))

(clojure.utils/defcomponent :s/animation
  (cdq.schema/malli-form [_ _schemas]
    cdq.malli/animation-schema)

  (cdq.db/edn->value [_ {:keys [frames frame-duration looping?]} _db c]
    (cdq.graphics.animation/create (map #(edn->sprite c %) frames)
                                   :frame-duration frame-duration
                                   :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(clojure.utils/defcomponent :s/one-to-one
  (cdq.schema/malli-form [[_ property-type] _schemas]
    (cdq.malli/qualified-keyword-schema (type->id-namespace property-type)))
  (cdq.db/edn->value [_ property-id db c]
    (clojure.context/build c property-id)))

(clojure.utils/defcomponent :s/one-to-many
  (cdq.schema/malli-form [[_ property-type] _schemas]
    (cdq.malli/set-schema (cdq.malli/qualified-keyword-schema (type->id-namespace property-type))))
  (cdq.db/edn->value [_ property-ids db c]
    (set (map #(clojure.context/build c %) property-ids))))

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
  (let [render-fns [(fn [{:keys [clojure.graphics/world-viewport
                                 cdq.context/player-eid]
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
                                 cdq.context/tiled-map
                                 cdq.context/raycaster
                                 cdq.context/explored-tile-corners]
                          :as context}]
                      (clojure.context/draw-tiled-map context
                                                  tiled-map
                                                  (cdq.graphics.tiled-map/tile-color-setter raycaster
                                                                                            explored-tile-corners
                                                                                            (clojure.graphics.camera/position (:camera world-viewport))))
                      context)
                    (fn [context]
                      (let [render-fns [cdq.graphics/render-before-entities
                                        cdq.graphics/render-entities
                                        cdq.graphics/render-after-entities]]
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
                    (fn [{:keys [cdq.context/player-eid] :as c}]
                      (cdq.entity.state/manual-tick (cdq.entity/state-obj @player-eid) c)
                      c)
                    (fn [{:keys [cdq.context/mouseover-eid
                                 cdq.context/player-eid] :as c}]
                      (let [new-eid (if (clojure.context/mouse-on-actor? c)
                                      nil
                                      (let [player @player-eid
                                            hits (remove #(= (:z-order @%) :z-order/effect)
                                                         (cdq.context/point->entities c (clojure.context/world-mouse-position c)))]
                                        (->> cdq.context/render-z-order
                                             (clojure.utils/sort-by-order hits #(:z-order @%))
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
                                                               (not (or (clojure.input/key-just-pressed? :p)
                                                                        (clojure.input/key-pressed? :space))))))))
                    (fn [c]
                      (if (:cdq.context/paused? c)
                        c
                        (-> c
                            update-time
                            update-potential-fields
                            update-entities)))
                    (fn [c]
                      ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                      (doseq [eid (filter (comp :entity/destroyed? deref)
                                          (cdq.context/all-entities c))]
                        (cdq.context/remove-entity c eid)
                        (doseq [component @eid]
                          (cdq.context/destroy! component eid c)))
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
                                     (run! (partial cdq.schema/validate! schemas) properties)
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
                    [:clojure.graphics/shape-drawer (fn [{:keys [clojure.graphics/batch
                                                                 clojure.graphics/shape-drawer-texture]} _config]
                                                      (space.earlygrey.shapedrawer.ShapeDrawer. batch
                                                                                                (clojure.graphics.2d.texture-region/create shape-drawer-texture 1 0 1 1)))]
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
                    [:clojure.graphics/default-font [cdq.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                                       :size 16
                                                                                       :quality-scaling 2}]]
                    [:clojure.graphics/world-unit-scale [cdq.graphics.world-unit-scale/create 48]]
                    [:clojure.graphics/tiled-map-renderer [clojure.graphics.tiled-map-renderer/create]]
                    [:clojure.graphics/ui-viewport [cdq.graphics.ui-viewport/create {:width 1440 :height 900}]]
                    [:clojure.graphics/world-viewport [cdq.graphics.world-viewport/create {:width 1440 :height 900}]]
                    [:clojure.context/stage [clojure.ui/setup-stage! {:skin-scale :x1
                                                                      :actors [[cdq.ui.dev-menu/create]
                                                                               [cdq.ui.actionbar/create]
                                                                               [cdq.ui.hp-mana-bar/create]
                                                                               [cdq.ui.windows/create [cdq.ui.entity-info-window/create
                                                                                                       cdq.widgets.inventory/create]]
                                                                               [cdq.ui.player-state/create]
                                                                               [cdq.ui.player-message/actor]]}]]
                    [:clojure.context/elapsed-time [cdq.time/create]]
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
