(ns cdq.application
  (:require [cdq.assets :as assets]
            [cdq.context :as context]
            [cdq.data.grid2d :as g2d]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.gdx.interop :as interop]
            [cdq.graphics :as graphics]
            cdq.graphics.animation
            [cdq.graphics.camera :as camera]
            [cdq.graphics.shape-drawer :as shape-drawer]
            cdq.graphics.sprite
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.grid :as grid]
            [cdq.input :as input]
            [cdq.line-of-sight :as los]
            [cdq.property :as property]
            [cdq.math.raycaster :as raycaster]
            [cdq.schema :as schema]
            cdq.time
            cdq.potential-fields
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            cdq.world
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import (cdq StageWithState OrthogonalTiledMapRenderer)
           (clojure.lang ILookup)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils Disposable ScreenUtils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport FitViewport Viewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- player-state-input! [{:keys [cdq.context/player-eid]
                             :as context}]
  (entity/manual-tick (entity/state-obj @player-eid) context)
  context)

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn- assoc-active-entities [{:keys [cdq.context/content-grid
                                      cdq.context/player-eid]
                               :as context}]
  (assoc context :cdq.game/active-entities (active-entities content-grid @player-eid)))

(defn- set-camera-on-player!
  [{:keys [cdq.graphics/world-viewport
           cdq.context/player-eid]
    :as context}]
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
  context)

(defn- clear-screen! [context]
  (ScreenUtils/clear Color/BLACK)
  context)

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

(defn- render-tiled-map! [{:keys [cdq.graphics/world-viewport
                                  cdq.context/tiled-map
                                  cdq.context/raycaster
                                  cdq.context/explored-tile-corners]
                           :as context}]
  (tiled-map-renderer/draw context
                           tiled-map
                           (tile-color-setter raycaster
                                              explored-tile-corners
                                              (camera/position (:camera world-viewport))))
  context)

(def ^:private render-fns
  '[(cdq.render.draw-on-world-view.before-entities/render)
    (cdq.render.draw-on-world-view.entities/render-entities
     {:below {:entity/mouseover? cdq.render.draw-on-world-view.entities/draw-faction-ellipse
              :player-item-on-cursor cdq.render.draw-on-world-view.entities/draw-world-item-if-exists
              :stunned cdq.render.draw-on-world-view.entities/draw-stunned-circle}
      :default {:entity/image cdq.render.draw-on-world-view.entities/draw-image-as-of-body
                :entity/clickable cdq.render.draw-on-world-view.entities/draw-text-when-mouseover-and-text
                :entity/line-render cdq.render.draw-on-world-view.entities/draw-line}
      :above {:npc-sleeping cdq.render.draw-on-world-view.entities/draw-zzzz
              :entity/string-effect cdq.render.draw-on-world-view.entities/draw-text
              :entity/temp-modifier cdq.render.draw-on-world-view.entities/draw-filled-circle-grey}
      :info {:entity/hp cdq.render.draw-on-world-view.entities/draw-hpbar-when-mouseover-and-not-full
             :active-skill cdq.render.draw-on-world-view.entities/draw-skill-image-and-active-effect}})
    (cdq.render.draw-on-world-view.after-entities/render)])

(defn- draw-with! [{:keys [^Batch cdq.graphics/batch
                           cdq.graphics/shape-drawer]
                    :as context}
                   viewport
                   unit-scale
                   draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera viewport)))
  (.begin batch)
  (shape-drawer/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc context :cdq.context/unit-scale unit-scale))))
  (.end batch))

(defn- draw-on-world-view* [{:keys [cdq.graphics/world-unit-scale
                                    cdq.graphics/world-viewport]
                             :as context}
                            render-fn]
  (draw-with! context
              world-viewport
              world-unit-scale
              render-fn))

(defn- draw-on-world-view! [context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)

(defn- stage-draw! [{:keys [^StageWithState cdq.context/stage]
                     :as context}]
  (set! (.applicationState stage) (assoc context :cdq.context/unit-scale 1))
  (Stage/.draw stage)
  context)

(defn- stage-act! [{:keys [^StageWithState cdq.context/stage]
                    :as context}]
  (set! (.applicationState stage) context)
  (Stage/.act stage)
  context)

(defn- update-mouseover-entity! [{:keys [cdq.context/grid
                                         cdq.context/mouseover-eid
                                         cdq.context/player-eid
                                         cdq.graphics/world-viewport
                                         cdq.context/stage]
                                  :as context}]
  (let [new-eid (if (stage/mouse-on-actor? stage)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (graphics/world-mouse-position world-viewport)))]
                    (->> cdq.world/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? context player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc context :cdq.context/mouseover-eid new-eid)))

(defn- set-paused-flag [{:keys [cdq.context/player-eid
                                context/entity-components
                                error ; FIXME ! not `::` keys so broken !
                                ]
                         :as context}]
  (let [pausing? true]
    (assoc context :cdq.context/paused? (or error
                                            (and pausing?
                                                 (get-in entity-components [(cdq.entity/state-k @player-eid) :pause-game?])
                                                 (not (or (input/key-just-pressed? :p)
                                                          (input/key-pressed?      :space))))))))

(defn- update-time [context]
  (let [delta-ms (min (.getDeltaTime Gdx/graphics)
                      cdq.time/max-delta)]
    (-> context
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defn- update-potential-fields! [{:keys [cdq.context/factions-iterations
                                         cdq.context/grid
                                         world/potential-field-cache
                                         cdq.game/active-entities]
                                  :as context}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick potential-field-cache
                               grid
                               faction
                               active-entities
                               max-iterations))
  context)

(defn- tick-entities! [{:keys [cdq.game/active-entities]
                        :as context}]
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
               (entity/tick! [k v] eid context))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (stage/error-window! (:cdq.context/stage context) t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  context)

(defn- when-not-paused! [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f]
              (f context))
            context
            [update-time
             update-potential-fields!
             tick-entities!])))

(defn- remove-destroyed-entities! [{:keys [cdq.context/entity-ids
                                           context/entity-components]
                                    :as context}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (doseq [component context]
      (context/remove-entity component eid))
    (doseq [[k v] @eid
            :let [destroy! (get-in entity-components [k :destroy!])]
            :when destroy!]
      (destroy! v eid context)))
  context)

(defn- camera-controls! [{:keys [cdq.graphics/world-viewport]
                          :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)

(defn- window-controls! [{:keys [cdq.context/stage]
                          :as context}]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (group/children (:windows stage))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible % false) windows))))
  context)

(defrecord Cursors []
  Disposable
  (dispose [this]
    (run! Disposable/.dispose (vals this))))

(defn- load-cursors [config]
  (map->Cursors
   (utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
            cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
        (.dispose pixmap)
        cursor))
    config)))

(defn- font-params [{:keys [size]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [file-handle params]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))

(defn- load-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-stage! [config batch viewport]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case (:skin-scale config)
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (let [stage (proxy [StageWithState ILookup] [viewport batch]
                (valAt
                  ([id]
                   (group/find-actor-with-id (StageWithState/.getRoot this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (StageWithState/.getRoot this) id)
                       not-found))))]
    (.setInputProcessor Gdx/input stage)
    stage))

(defn- tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- fit-viewport
  "A ScalingViewport that uses Scaling.fit so it keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the remaining space."
  [width height camera & {:keys [center-camera?]}]
  {:pre [width height]}
  (proxy [FitViewport ILookup] [width height camera]
    (valAt
      ([key]
       (interop/k->viewport-field this key))
      ([key _not-found]
       (interop/k->viewport-field this key)))))

(defn- world-viewport [world-unit-scale config]
  {:pre [world-unit-scale]}
  (let [camera (OrthographicCamera.)
        world-width  (* (:width  config) world-unit-scale)
        world-height (* (:height config) world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defmethod schema/edn->value :s/sound [_ sound-name {:keys [cdq/assets]}]
  (assets/sound assets sound-name))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (cdq.graphics.sprite/from-sheet (cdq.graphics.sprite/sheet c file tilew tileh)
                                      [(int (/ sprite-x tilew))
                                       (int (/ sprite-y tileh))]
                                      c))
    (cdq.graphics.sprite/create c file)))

(defmethod schema/edn->value :s/image [_ edn c]
  (edn->sprite c edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} c]
  (cdq.graphics.animation/create (map #(edn->sprite c %) frames)
                                     :frame-duration frame-duration
                                     :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id {:keys [cdq/db] :as context}]
  (db/build db property-id context))

(defmethod schema/edn->value :s/one-to-many [_ property-ids {:keys [cdq/db] :as context}]
  (set (map #(db/build db % context) property-ids)))

(defn- validate-properties! [properties schemas]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! schemas (property/type %) %) properties))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- build* [{:keys [cdq/schemas] :as c} property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* c v)
                         v)]
                 (try (schema/edn->value schema v c)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn- async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint
             with-out-str
             (spit file)))))))

(defrecord DB []
  db/DB
  (async-write-to-file! [{:keys [db/data db/properties-file]}]
    ; TODO validate them again!?
    (->> data
         vals
         (sort-by property/type)
         (map recur-sort-map)
         doall
         (async-pprint-spit! properties-file)))

  (update [{:keys [db/data] :as db}
           {:keys [property/id] :as property}
           schemas]
    {:pre [(contains? property :property/id)
           (contains? data id)]}
    (schema/validate! schemas (property/type property) property)
    (clojure.core/update db :db/data assoc id property)) ; assoc-in ?

  (delete [{:keys [db/data] :as db} property-id]
    {:pre [(contains? data property-id)]}
    (clojure.core/update db dissoc :db/data property-id)) ; dissoc-in ?

  (get-raw [{:keys [db/data]} id]
    (utils/safe-get data id))

  (all-raw [{:keys [db/data]} property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build [this id context]
    (build* context (db/get-raw this id)))

  (build-all [this property-type context]
    (map (partial build* context)
         (db/all-raw this property-type))))

(defn- create-db [schemas]
  (let [properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! properties schemas)
    (map->DB {:db/data (zipmap (map :property/id properties) properties)
              :db/properties-file properties-file})))

(defn- create-initial-context! [config]
  (let [batch (SpriteBatch.)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ (:world-unit-scale config)))
        ui-viewport (fit-viewport (:width  (:ui-viewport config))
                                  (:height (:ui-viewport config))
                                  (OrthographicCamera.))
        schemas (-> (:schemas config) io/resource slurp edn/read-string)]
    {:cdq/assets (assets/create (:assets config))
     ;;
     :cdq.graphics/batch batch
     :cdq.graphics/cursors (load-cursors (:cursors config))
     :cdq.graphics/default-font (load-font (:default-font config))
     :cdq.graphics/shape-drawer (shape-drawer/create batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
     :cdq.graphics/shape-drawer-texture shape-drawer-texture
     :cdq.graphics/tiled-map-renderer (tiled-map-renderer batch world-unit-scale)
     :cdq.graphics/world-unit-scale world-unit-scale
     :cdq.graphics/world-viewport (world-viewport world-unit-scale (:world-viewport config))
     ;;
     :cdq.graphics/ui-viewport ui-viewport
     :cdq.context/stage (create-stage! (:ui config) batch ui-viewport)
     ;;
     :cdq/schemas schemas
     :cdq/db (create-db schemas)
     ;;
     }))

(def state
  "Do not call `swap!`, instead use `post-runnable!`, as the main game loop has side-effects and should not be retried.

  (Should probably make this private and have a `get-state` function)"
  (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-pipeline (map requiring-resolve (:create-pipeline config))]
    (doseq [ns (:requires config)]
      #_(println "requiring " ns)
      (require ns))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource (:dock-icon (:mac-os config)))))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state (reduce (fn [context f]
                                                    (f context config))
                                                  (create-initial-context! config)
                                                  create-pipeline)))

                          (dispose []
                            (doseq [[k obj] @state]
                              (if (instance? Disposable obj)
                                (do
                                 #_(println "Disposing:" k)
                                 (Disposable/.dispose obj))
                                #_(println "Not Disposable: " k ))))

                          (render []
                            (swap! state (fn [context]
                                           (reduce (fn [context f]
                                                     (f context))
                                                   context
                                                   [assoc-active-entities
                                                    set-camera-on-player!
                                                    clear-screen!
                                                    render-tiled-map!
                                                    draw-on-world-view!
                                                    stage-draw!
                                                    stage-act!
                                                    player-state-input!
                                                    update-mouseover-entity!
                                                    set-paused-flag
                                                    when-not-paused!

                                                    ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                                                    remove-destroyed-entities!

                                                    camera-controls!
                                                    window-controls!]))))

                          (resize [width height]
                            (let [context @state]
                              (Viewport/.update (:cdq.graphics/ui-viewport    context) width height true)
                              (Viewport/.update (:cdq.graphics/world-viewport context) width height false))))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width  (:windowed-mode config))
                                            (:height (:windowed-mode config)))
                          (.setForegroundFPS (:foreground-fps config))))))

(defn post-runnable!
  "`f` should be a `(fn [context])`.

  Is executed after the main-loop, in order not to interfere with it."
  [f]
  (.postRunnable Gdx/app (fn [] (f @state))))
