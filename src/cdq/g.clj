(ns cdq.g
  (:require [cdq.db :as db]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.g.world]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.stage :as stage]
            [cdq.tiled :as tiled]
            [cdq.math :refer [circle->outer-rectangle]]
            [cdq.math.raycaster :as raycaster]
            [cdq.utils :as utils :refer [sort-by-order
                                         tile->middle
                                         pretty-pst
                                         bind-root]]
            [cdq.world :as world]
            [cdq.world.grid :as grid]
            cdq.world.potential-fields
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx ApplicationAdapter Gdx Input$Keys)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.utils Disposable ScreenUtils SharedLibraryLoader Os)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(def asset-folder "resources/")
(def asset-type-extensions {Sound   #{"wav"}
                            Texture #{"png" "bmp"}})

(defn- load-assets []
  (let [manager (proxy [AssetManager IFn] []
                  (invoke [path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this ^String path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[file asset-type] (for [[asset-type extensions] asset-type-extensions
                                    file (map #(str/replace-first % asset-folder "")
                                              (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files asset-folder))
                                                     result []]
                                                (cond (nil? file)
                                                      result

                                                      (.isDirectory file)
                                                      (recur (concat remaining (.list file)) result)

                                                      (extensions (.extension file))
                                                      (recur remaining (conj result (.path file)))

                                                      :else
                                                      (recur remaining result))))]
                                [file asset-type])]
      (.load manager ^String file ^Class asset-type))
    (.finishLoading manager)
    manager))

(defn- spawn-enemies! []
  (doseq [props (for [[position creature-id] (tiled/positions-with-property (:tiled-map ctx/world) :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (world/spawn-creature (update props :position tile->middle))))

(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor-key (state/cursor new-state-obj)]
                                                     (graphics/set-cursor! ctx/graphics cursor-key)))
                                 :skill-added! (fn [skill]
                                                 (stage/add-skill! ctx/stage skill))
                                 :skill-removed! (fn [skill]
                                                   (stage/remove-skill! ctx/stage skill))
                                 :item-set! (fn [inventory-cell item]
                                              (stage/set-item! ctx/stage inventory-cell item))
                                 :item-removed! (fn [inventory-cell]
                                                  (stage/remove-item! ctx/stage inventory-cell))}
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- reset-game! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (bind-root #'ctx/stage (stage/create!))
  (bind-root #'ctx/world (cdq.g.world/create ((requiring-resolve world-fn)
                                              (db/build-all ctx/db :properties/creatures))))
  (spawn-enemies!)
  (bind-root #'ctx/player-eid (world/spawn-creature (player-entity-props (:start-position ctx/world)))))

(bind-root #'ctx/reset-game! reset-game!)

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color Color/BLACK)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? Color/WHITE base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              Color/WHITE))))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn- draw-before-entities! []
  (let [g ctx/graphics
        cam (:camera (:world-viewport g))
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/draw-grid g
                          (int left-x) (int bottom-y)
                          (inc (int (:width  (:world-viewport g))))
                          (+ 2 (int (:height (:world-viewport g))))
                          1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell ((:grid ctx/world) [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/draw-filled-rectangle g x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test [g]
  (let [position (graphics/world-mouse-position g)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/draw-circle g position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells (:grid ctx/world) circle))]
      (graphics/draw-rectangle g x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/draw-rectangle g x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [g]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position g))
          cell ((:grid ctx/world) [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/draw-rectangle g x y 1 1
                                 (case (:movement @cell)
                                   :air  [1 1 0 0.5]
                                   :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test ctx/graphics)
  (highlight-mouseover-tile ctx/graphics))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [g entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/draw-rectangle g x y (:width entity) (:height entity) color)))

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})

(defn- render-entities! []
  (let [entities (map deref (:active-entities ctx/world))
        player @ctx/player-eid
        g ctx/graphics]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              world/render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (world/line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect g entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity g))
       (catch Throwable t
         (draw-body-rect g entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (stage/mouse-on-actor? ctx/stage)
                  nil
                  (let [player @ctx/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities (:grid ctx/world)
                                                           (graphics/world-mouse-position ctx/graphics)))]
                    (->> world/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(world/line-of-sight? player @%))
                         first)))]
    (when-let [eid ctx/mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'ctx/mouseover-eid new-eid)))

(def pausing? true)

(defn- pause-game? []
  (or #_error
      (and pausing?
           (state/pause-game? (entity/state-obj @ctx/player-eid))
           (not (or (.isKeyJustPressed Gdx/input Input$Keys/P)
                    (.isKeyPressed     Gdx/input Input$Keys/SPACE))))))

(defn- update-potential-fields! [{:keys [potential-field-cache
                                         grid
                                         active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.world.potential-fields/tick potential-field-cache
                                     grid
                                     faction
                                     active-entities
                                     max-iterations)))

(defn- tick-entities! [{:keys [active-entities]}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (entity/tick! [k v] eid))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (stage/show-error-window! ctx/stage t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! [camera]
  (let [zoom-speed 0.025]
    (when (.isKeyPressed Gdx/input Input$Keys/MINUS)  (camera/inc-zoom camera    zoom-speed))
    (when (.isKeyPressed Gdx/input Input$Keys/EQUALS) (camera/inc-zoom camera (- zoom-speed)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (bind-root #'ctx/db (db/create))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource "moon.png")))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (bind-root #'ctx/assets (load-assets))
                            (bind-root #'ctx/graphics (graphics/create (:graphics config)))
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (Disposable/.dispose ctx/assets)
                            (Disposable/.dispose ctx/graphics)
                            ; TODO vis-ui dispose
                            ; TODO dispose world tiled-map/level resources?
                            )

                          (render []
                            (alter-var-root #'ctx/world world/cache-active-entities)
                            (graphics/set-camera-position! ctx/graphics (:position @ctx/player-eid))
                            (ScreenUtils/clear Color/BLACK)
                            (graphics/draw-tiled-map ctx/graphics
                                                     (:tiled-map ctx/world)
                                                     (tile-color-setter (:raycaster ctx/world)
                                                                        (:explored-tile-corners ctx/world)
                                                                        (camera/position (:camera (:world-viewport ctx/graphics)))))
                            (graphics/draw-on-world-view! ctx/graphics
                                                          (fn []
                                                            (draw-before-entities!)
                                                            (render-entities!)
                                                            (draw-after-entities!)))
                            (stage/draw! ctx/stage)
                            (stage/act! ctx/stage)
                            (state/manual-tick (entity/state-obj @ctx/player-eid))
                            (update-mouseover-entity!)
                            (bind-root #'ctx/paused? (pause-game?))
                            (when-not ctx/paused?
                              (let [delta-ms (min (.getDeltaTime Gdx/graphics) world/max-delta)]
                                (alter-var-root #'ctx/elapsed-time + delta-ms)
                                (bind-root #'ctx/delta-time delta-ms))
                              (update-potential-fields! ctx/world)
                              (tick-entities! ctx/world))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (world/remove-destroyed-entities! ctx/world)

                            (camera-controls! (:camera (:world-viewport ctx/graphics)))
                            (stage/check-window-controls! ctx/stage))

                          (resize [width height]
                            (graphics/resize! ctx/graphics width height)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle "Cyber Dungeon Quest")
                          (.setWindowedMode 1440 900)
                          (.setForegroundFPS 60)))))
