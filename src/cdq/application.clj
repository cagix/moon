(ns cdq.application
  (:require cdq.schemas
            clojure.context
            clojure.create
            clojure.db
            clojure.edn
            clojure.entity
            clojure.entity.state
            clojure.gdx.assets
            clojure.gdx.backends.lwjgl
            clojure.gdx.graphics.shape-drawer
            clojure.graphics
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
            clojure.world.graphics)
  (:import (com.badlogic.gdx.utils ScreenUtils)))

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

(def state (atom nil))

(defn post-runnable [f]
  (.postRunnable com.badlogic.gdx.Gdx/app #(f @state)))

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
                      (ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
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
                    [:clojure/assets clojure.gdx.assets/manager]
                    [:clojure.graphics/batch clojure.graphics/sprite-batch]
                    [:clojure.graphics/shape-drawer-texture (fn [_context _config]
                                                              (let [pixmap (doto (clojure.graphics.pixmap/create 1 1 clojure.graphics.pixmap/format-RGBA8888)
                                                                             (clojure.graphics.pixmap/set-color clojure.graphics.color/white)
                                                                             (clojure.graphics.pixmap/draw-pixel 0 0))
                                                                    texture (clojure.graphics.texture/create pixmap)]
                                                                (clojure.utils/dispose pixmap)
                                                                texture))]
                    [:clojure.graphics/shape-drawer clojure.gdx.graphics.shape-drawer/create]
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
    (clojure.gdx.backends.lwjgl/application
     {:icon "moon.png"
      :title "Cyber Dungeon Quest"
      :window-width 1440
      :window-height 900
      :foreground-fps 60
      :create (fn []
                (reset! state
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
      :dispose (fn []
                 (doseq [[k value] @state
                         :when (clojure.utils/disposable? value)]
                   ;(println "Disposing " k " - " value)
                   (clojure.utils/dispose value)))
      :render (fn []
                (swap! state (fn [context]
                               (reduce (fn [context f] (f context))
                                       context
                                       render-fns))))
      :resize (fn [width height]
                (let [context @state]
                  (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/ui-viewport    context) width height true)
                  (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/world-viewport context) width height false)))})))
