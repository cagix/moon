(ns cdq.g
  (:require [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.grid :as grid]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.math.raycaster :as raycaster]
            [cdq.math.shapes :refer [circle->outer-rectangle]]
            [cdq.math.vector2 :as v]
            cdq.potential-fields
            [cdq.property :as property]
            [cdq.schema :as schema]
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.menu :as ui.menu]
            [cdq.val-max :as val-max]
            [cdq.world.content-grid :as content-grid]
            [clojure.data.animation :as animation]
            [clojure.data.grid2d :as g2d]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.asset-manager :as asset-manager]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.shape-drawer :as shape-drawer]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.disposable :refer [dispose!]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.utils :as utils :refer [readable-number
                                             pretty-pst
                                             sort-by-order
                                             define-order
                                             safe-merge
                                             tile->middle
                                             pretty-pst
                                             with-err-str]]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor Group Stage)
           (com.badlogic.gdx.scenes.scene2d.ui Image Widget)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener)
           (com.badlogic.gdx.utils.viewport Viewport)))

(declare ^:private asset-manager)

(declare ^:private ^Batch batch
         ^:private ^Texture shape-drawer-texture
         ^:private shape-drawer
         ^:private cursors
         ^:private default-font
         ^:private world-unit-scale
         ^:private world-viewport
         ^:private get-tiled-map-renderer
         ^:private ^:dynamic *unit-scale*
         ui-viewport)

(declare ^:private ^Stage stage)

(declare player-message)

(declare ^:private -data
         ^:private -properties-file
         ^:private -schemas)

(declare elapsed-time)

(declare tiled-map
         ^:private content-grid
         ^:private entity-ids
         explored-tile-corners
         grid
         raycaster
         potential-field-cache
         player-eid
         active-entities
         delta-time)

(def mouseover-eid nil)

(declare paused?)

(defn- create-graphics! [{:keys [cursors
                                 default-font
                                 tile-size
                                 world-viewport
                                 ui-viewport]}]
  (.bindRoot #'batch (graphics/sprite-batch))
  (.bindRoot #'shape-drawer-texture (graphics/white-pixel-texture))
  (.bindRoot #'shape-drawer (shape-drawer/create batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
  (.bindRoot #'cursors (utils/mapvals
                        (fn [[file [hotspot-x hotspot-y]]]
                          (let [pixmap (graphics/pixmap (str "cursors/" file ".png"))
                                cursor (gdx/cursor pixmap hotspot-x hotspot-y)]
                            (dispose! pixmap)
                            cursor))
                        cursors))
  (.bindRoot #'default-font (graphics/truetype-font default-font))
  (.bindRoot #'world-unit-scale (float (/ tile-size)))
  (.bindRoot #'world-viewport (graphics/world-viewport world-unit-scale world-viewport))
  (.bindRoot #'get-tiled-map-renderer (memoize (fn [tiled-map]
                                                 (tiled-map-renderer/create tiled-map
                                                                            world-unit-scale
                                                                            batch))))
  (.bindRoot #'ui-viewport (graphics/fit-viewport (:width  ui-viewport)
                                                  (:height ui-viewport))))

(defn- dispose-graphics! []
  (.dispose batch)
  (.dispose shape-drawer-texture)
  (run! dispose! (vals cursors))
  (dispose! default-font))

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (graphics/unproject-mouse-position ui-viewport)))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (graphics/unproject-mouse-position world-viewport))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn- unit-dimensions [image]
  (if (bound? #'*unit-scale*)
    (:world-unit-dimensions image)
    (:pixel-dimensions image)))

(defn draw-image
  [{:keys [texture-region color] :as image} position]
  (graphics/draw-texture-region batch
                                texture-region
                                position
                                (unit-dimensions image)
                                0 ; rotation
                                color))

(defn draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image)]
    (graphics/draw-texture-region batch
                                  texture-region
                                  [(- (float x) (/ (float w) 2))
                                   (- (float y) (/ (float h) 2))]
                                  [w h]
                                  rotation
                                  color)))

(defn draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- set-camera-position! [position]
  (camera/set-position! (:camera world-viewport) position))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font scale x y text h-align up?]}]
  (graphics/draw-text! {:font (or font default-font)
                        :scale (* (float (if (bound? #'*unit-scale*) *unit-scale* 1))
                                  (float (or scale 1)))
                        :batch batch
                        :x x
                        :y y
                        :text text
                        :h-align h-align
                        :up? up?}))

(defn draw-ellipse [[x y] radius-x radius-y color]
  (shape-drawer/ellipse! shape-drawer x y radius-x radius-y color))

(defn draw-filled-ellipse [[x y] radius-x radius-y color]
  (shape-drawer/filled-ellipse! shape-drawer x y radius-x radius-y color))

(defn draw-circle [[x y] radius color]
  (shape-drawer/circle! shape-drawer x y radius color))

(defn draw-filled-circle [[x y] radius color]
  (shape-drawer/filled-circle! shape-drawer x y radius color))

(defn draw-arc [[center-x center-y] radius start-angle degree color]
  (shape-drawer/arc! shape-drawer center-x center-y radius start-angle degree color))

(defn draw-sector [[center-x center-y] radius start-angle degree color]
  (shape-drawer/sector! shape-drawer center-x center-y radius start-angle degree color))

(defn draw-rectangle [x y w h color]
  (shape-drawer/rectangle! shape-drawer x y w h color))

(defn draw-filled-rectangle [x y w h color]
  (shape-drawer/filled-rectangle! shape-drawer x y w h color))

(defn draw-line [[sx sy] [ex ey] color]
  (shape-drawer/line! shape-drawer sx sy ex ey color))

(defn with-line-width [width draw-fn]
  (shape-drawer/with-line-width shape-drawer width draw-fn))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (shape-drawer/grid! shape-drawer color))

(defn- draw-on-world-view! [f]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
  (.begin batch)
  (with-line-width world-unit-scale
    (fn []
      (binding [*unit-scale* world-unit-scale]
        (f))))
  (.end batch))

(defn set-cursor! [cursor-key]
  (gdx/set-cursor! (utils/safe-get cursors cursor-key)))

(defn- draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (tiled-map-renderer/draw! (get-tiled-map-renderer tiled-map)
                            tiled-map
                            color-setter
                            (:camera world-viewport)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1) ; = scale 1
      map->Sprite))

(defn sub-sprite [sprite [x y w h]]
  (sprite* (TextureRegion. ^TextureRegion (:texture-region sprite)
                           (int x)
                           (int y)
                           (int w)
                           (int h))))

(defn sprite-sheet [^Texture texture tilew tileh]
  {:image (sprite* (TextureRegion. texture))
   :tilew tilew
   :tileh tileh})

(defn from-sheet [{:keys [image tilew tileh]}
                  [x y]]
  (sub-sprite image
              [(* x tilew)
               (* y tileh)
               tilew
               tileh]))

(defn ->sprite [^Texture texture]
  (sprite* (TextureRegion. texture)))

(defn- init-stage! []
  (let [stage (proxy [Stage clojure.lang.ILookup] [ui-viewport batch]
                (valAt
                  ([id]
                   (ui/find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (ui/find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (gdx/set-input-processor! stage)
    (.bindRoot #'stage stage)))

(defn get-actor [id-keyword]
  (id-keyword stage))

(defn show-player-msg! [text]
  (swap! player-message assoc :text text :counter 0))

(defn mouse-on-actor? []
  (let [[x y] (mouse-position #_(Stage/.getViewport stage))]
    (Stage/.hit stage x y true)))

(defn add-actor [actor]
  (Stage/.addActor stage actor))

(defn get-inventory []
  (get (:windows stage) :inventory-window))

(defn get-action-bar []
  (action-bar/get-data stage))

(defn selected-skill []
  (action-bar/selected-skill (get-action-bar)))

(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor (ui/window {:title "Error"
                         :rows [[(ui/label (binding [*print-level* 3]
                                             (with-err-str
                                               (clojure.repl/pst throwable))))]]
                         :modal? true
                         :close-button? true
                         :close-on-escape? true
                         :center? true
                         :pack? true})))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (file-handle/list folder)
         result []]
    (cond (nil? file)
          result

          (file-handle/directory? file)
          (recur (concat remaining (file-handle/list file)) result)

          (extensions (file-handle/extension file))
          (recur remaining (conj result (file-handle/path file)))

          :else
          (recur remaining result))))

(defn- create-asset-manager! [folder]
  (let [assets (for [[asset-type extensions] {com.badlogic.gdx.audio.Sound #{"wav"}
                                              com.badlogic.gdx.graphics.Texture #{"png" "bmp"}}
                     file (map #(str/replace-first % folder "")
                               (recursively-search (gdx/internal folder) extensions))]
                 [file asset-type])]
    (.bindRoot #'asset-manager (asset-manager/create assets))))

(defn- dispose-asset-manager! []
  (dispose! asset-manager))

(defn assets-of-type [asset-type]
  (asset-manager/all-of-type asset-manager asset-type))

(defn asset [path]
  (asset-manager/get asset-manager path))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

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

(defn- validate-properties! [properties]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! -schemas (property/type %) %) properties))

(defn- create-db! []
  (let [schemas (-> "schema.edn" io/resource slurp edn/read-string)
        properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (.bindRoot #'-schemas schemas)
    (validate-properties! properties)
    (.bindRoot #'-data (zipmap (map :property/id properties) properties))
    (.bindRoot #'-properties-file properties-file)))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- build* [property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of -schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* v)
                         v)]
                 (try (schema/edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn- async-write-to-file! []
  ; TODO validate them again!?
  (->> -data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! -properties-file)))

(defn update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? -data id)]}
  (schema/validate! -schemas (property/type property) property)
  (alter-var-root #'-data assoc id property)
  (async-write-to-file!))

(defn delete! [property-id]
  {:pre [(contains? -data property-id)]}
  (alter-var-root #'-data dissoc property-id)
  (async-write-to-file!))

(defn get-raw [property-id]
  (utils/safe-get -data property-id))

(defn all-raw [property-type]
  (->> (vals -data)
       (filter #(= property-type (property/type %)))))

(defn build [property-id]
  (build* (get-raw property-id)))

(defn build-all [property-type]
  (map build* (all-raw property-type)))

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (from-sheet (sprite-sheet (asset file) tilew tileh)
                           [(int (/ sprite-x tilew))
                            (int (/ sprite-y tileh))]))
    (->sprite (asset file))))

(defmethod schema/edn->value :s/image [_ edn]
  (edn->sprite edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (animation/create (map edn->sprite frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id]
  (build property-id))

(defmethod schema/edn->value :s/one-to-many [_ property-ids]
  (set (map build property-ids)))

(defn play-sound! [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       asset
       sound/play!))

(defn ->timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn timer-reset [{:keys [duration] :as timer}]
  (assoc timer :stop-time (+ elapsed-time duration)))

(defn timer-ratio [{:keys [duration stop-time] :as timer}]
  {:post [(<= 0 % 1)]}
  (if (stopped? timer)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(defn send-event!
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/fsm-event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid]))]]
           (when (:entity/player? @eid)
             (when-let [cursor-key (state/cursor new-state-obj)]
               (set-cursor! cursor-key)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (state/exit!  old-state-obj)
           (state/enter! new-state-obj)))))))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npc/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npc/player movement 'state' and no movement 'component' necessary !
; for projectiles inside projectile update !?
(defn set-movement [eid movement-vector]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

(defn mark-destroyed [eid]
  (swap! eid assoc :entity/destroyed? true))

(defn toggle-inventory-window []
  (ui/toggle-visible! (get-inventory)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (get-actor ::modal)))
  (add-actor (ui/window {:title title
                         :rows [[(ui/label text)]
                                [(ui/text-button button-text
                                                 (fn []
                                                   (Actor/.remove (get-actor ::modal))
                                                   (on-click)))]]
                         :id ::modal
                         :modal? true
                         :center-position [(/ (:width  ui-viewport) 2)
                                           (* (:height ui-viewport) (/ 3 4))]
                         :pack? true})))

(defn add-skill [eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar/add-skill! (get-action-bar) skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar/remove-skill! (get-action-bar) skill))
  (swap! eid update :entity/skills dissoc id))

(defn- add-text-effect* [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter timer-reset))
           {:text text
            :counter (->timer 0.4)})))

(defn add-text-effect! [eid text]
  (swap! eid add-text-effect* text))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(defn- set-cells! [grid eid]
  (let [cells (grid/rectangle->cells grid @eid)]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn- remove-from-cells! [eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/rectangle->cells grid rectangle)
    [(grid [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
            (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [grid eid]
  (let [cells (rectangle->occupied-cells grid @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn- add-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))

  (content-grid/update-entity! content-grid eid)

  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))

  (content-grid/remove-entity! eid)

  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [eid]
  (content-grid/update-entity! content-grid eid)

  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defn- remove-destroyed-entities! []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (remove-entity! eid)
    (doseq [component @eid]
      (entity/destroy! component eid))))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def z-orders [:z-order/on-ground
               :z-order/ground
               :z-order/flying
               :z-order/effect])

(def render-z-order (define-order z-orders))

(defrecord Body [position
                 left-bottom
                 width
                 height
                 half-width
                 half-height
                 radius
                 collides?
                 z-order
                 rotation-angle])

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v])))
          {}
          components))

(def id-counter (atom 0))

(defn- spawn-entity [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (swap! id-counter inc))
                                      create-vs))))]
    (add-entity! eid)
    (doseq [component @eid]
      (entity/create! component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [position {:keys [tx/sound entity/animation]}]
  (play-sound! sound)
  (spawn-entity position
                effect-body-props
                {:entity/animation animation
                 :entity/delete-after-animation-stopped? true}))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn spawn-creature [{:keys [position creature-id components]}]
  (let [props (build creature-id)]
    (spawn-entity position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn spawn-item [position item]
  (spawn-entity position
                {:width 0.75
                 :height 0.75
                 :z-order :z-order/on-ground}
                {:entity/image (:entity/image item)
                 :entity/item item
                 :entity/clickable {:type :clickable/item
                                    :text (:property/pretty-name item)}}))

(defn delayed-alert [position faction duration]
  (spawn-entity position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (->timer duration)
                  :faction faction}}))

(defn line-render [{:keys [start end duration color thick?]}]
  (spawn-entity start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn spawn-projectile [{:keys [position direction faction]}
                        {:keys [entity/image
                                projectile/max-range
                                projectile/speed
                                entity-effects
                                projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (spawn-entity position
                  {:width size
                   :height size
                   :z-order :z-order/flying
                   :rotation-angle (v/angle-from-vector direction)}
                  {:entity/movement {:direction direction
                                     :speed speed}
                   :entity/image image
                   :entity/faction faction
                   :entity/delete-after-duration (/ max-range speed)
                   :entity/destroy-audiovisual :audiovisuals/hit-wall
                   :entity/projectile-collision {:entity-effects entity-effects
                                                 :piercing? piercing?}})))

(def ^:private shout-radius 4)

(defn friendlies-in-radius [grid position faction]
  (->> {:position position
        :radius shout-radius}
       (grid/circle->entities grid)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn world-item? []
  (not (mouse-on-actor?)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [entity]
  (placement-point (:position entity)
                   (world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- spawn-enemies! []
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature (update props :position tile->middle))))

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

(defn- create-world-state! [{:keys [tiled-map start-position]}]
  (.bindRoot #'tiled-map tiled-map)
  (.bindRoot #'content-grid (content-grid/create {:cell-size 16
                                                  :width  (tiled/tm-width  tiled-map)
                                                  :height (tiled/tm-height tiled-map)}))
  (.bindRoot #'explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                            (tiled/tm-height tiled-map)
                                                            (constantly false))))
  (.bindRoot #'entity-ids (atom {}))
  (.bindRoot #'grid (create-grid tiled-map))
  (.bindRoot #'raycaster (create-raycaster grid))
  (.bindRoot #'potential-field-cache (atom nil))
  (spawn-enemies!)
  (.bindRoot #'player-eid (spawn-creature (player-entity-props start-position))))

(defn- cache-active-entities!
  "Expensive operation.

  Active entities are those which are nearby the position of the player and about one screen away."
  []
  (.bindRoot #'active-entities (content-grid/active-entities content-grid @player-eid)))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? world-viewport target))
       (not (and los-checks?
                 (raycaster/blocked? raycaster (:position source) (:position target))))))

(defn creatures-in-los-of-player []
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @player-eid @%))
       (remove #(:entity/player? @%))))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private inventory-cell-size 48)
(def ^:private inventory-droppable-color   [0   0.6 0 0.8])
(def ^:private inventory-not-allowed-color [0.6 0   0 0.8])

(defn- draw-inventory-cell-rect! [player-entity x y mouseover? cell]
  (draw-rectangle x y inventory-cell-size inventory-cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  inventory-droppable-color
                  inventory-not-allowed-color)]
      (draw-filled-rectangle (inc x) (inc y) (- inventory-cell-size 2) (- inventory-cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-inventory-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [^Actor this this]
        (draw-inventory-cell-rect! @player-eid
                                   (.getX this)
                                   (.getY this)
                                   (ui/hit this (mouse-position))
                                   (.getUserObject (.getParent this)))))))

(def ^:private slot->y-sprite-idx
  #:inventory.slot {:weapon   0
                    :shield   1
                    :rings    2
                    :necklace 3
                    :helm     4
                    :cloak    5
                    :chest    6
                    :leg      7
                    :glove    8
                    :boot     9
                    :bag      10}) ; transparent

(defn- slot->sprite-idx [slot]
  [21 (+ (slot->y-sprite-idx slot) 2)])

(defn- slot->sprite [slot]
  (from-sheet (sprite-sheet (asset "images/items.png") 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     ui/texture-region-drawable)]
    (BaseDrawable/.setMinSize drawable
                              (float inventory-cell-size)
                              (float inventory-cell-size))
    (TextureRegionDrawable/.tint drawable
                                 (Color. (float 1) (float 1) (float 1) (float 0.4)))))

(defn- ->inventory-cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (ui/image-widget (slot->background slot)
                                      {:id :image})
        stack (ui/ui-stack [(draw-inventory-rect-actor)
                            image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (proxy [ClickListener] []
                          (clicked [_event _x _y]
                            (entity/clicked-inventory-cell (entity/state-obj @player-eid)
                                                           cell))))
    stack))

(defn- inventory-table []
  (ui/table {:id ::table
             :rows (concat [[nil nil
                             (->inventory-cell :inventory.slot/helm)
                             (->inventory-cell :inventory.slot/necklace)]
                            [nil
                             (->inventory-cell :inventory.slot/weapon)
                             (->inventory-cell :inventory.slot/chest)
                             (->inventory-cell :inventory.slot/cloak)
                             (->inventory-cell :inventory.slot/shield)]
                            [nil nil
                             (->inventory-cell :inventory.slot/leg)]
                            [nil
                             (->inventory-cell :inventory.slot/glove)
                             (->inventory-cell :inventory.slot/rings :position [0 0])
                             (->inventory-cell :inventory.slot/rings :position [1 0])
                             (->inventory-cell :inventory.slot/boot)]]
                           (for [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
                               (->inventory-cell :inventory.slot/bag :position [x y]))))}))

(defn- create-inventory-widget [position]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- inventory-cell-widget [cell]
  (get (::table (get (get-actor :windows) :inventory-window)) cell))

(defn- set-item-image-in-widget [cell item]
  (let [cell-widget (inventory-cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (ui/texture-region-drawable (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable
                              (float inventory-cell-size)
                              (float inventory-cell-size))
    (Image/.setDrawable image-widget drawable)
    (ui/add-tooltip! cell-widget #(info/text item))))

(defn- remove-item-from-widget [cell]
  (let [cell-widget (inventory-cell-widget cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (ui/remove-tooltip! cell-widget)))

(defn set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item eid cell)
            (set-item eid cell (update cell-item :count + (:count item))))))

(defn pickup-item [eid item]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (stack-item eid cell item)
      (set-item eid cell item))))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text []
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid mouseover-eid]
                (info/text [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid mouseover-eid]
    (info/text ; don't use select-keys as it loses Entity record type
               (apply dissoc @eid disallowed-keys))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (ui-actor {:act (fn []
                                        (.setText label (str (->label-text)))
                                        (.pack window))}))
    window))

(defn- render-infostr-on-bar [infostr x y h]
  (draw-text {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (->sprite (asset "images/rahmen.png"))
        hpcontent   (->sprite (asset "images/hp.png"))
        manacontent (->sprite (asset "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (draw-image rahmen [x y])
                            (draw-image (sub-sprite contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn- draw-player-message []
  (when-let [text (:text @player-message)]
    (draw-text {:x (/ (:width     ui-viewport) 2)
                :y (+ (/ (:height ui-viewport) 2) 200)
                :text text
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when (:text @player-message)
    (swap! player-message update :counter + (gdx/delta-time))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @player-eid))}))

(declare dev-menu-config)

(defn- reset-game! [world-fn]
  (.bindRoot #'player-message (atom {:duration-seconds 1.5}))
  (.clear stage)
  (run! add-actor [(ui.menu/create (dev-menu-config))
                   (action-bar/create)
                   (hp-mana-bar [(/ (:width ui-viewport) 2)
                                 80 ; action-bar-icon-size
                                 ])
                   (ui/group {:id :windows
                              :actors [(entity-info-window [(:width ui-viewport) 0])
                                       (create-inventory-widget [(:width  ui-viewport)
                                                                 (:height ui-viewport)])]})
                   (player-state-actor)
                   (player-message-actor)])
  (.bindRoot #'elapsed-time 0)
  (create-world-state! ((requiring-resolve world-fn) (build-all :properties/creatures))))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] (reset-game! world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys @#'-schemas)))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  ((requiring-resolve 'cdq.editor/open-main-window!) property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (:entity/id entity)))
                    :icon (asset "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (readable-number elapsed-time) " seconds"))
                    :icon (asset "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [] paused?)}
                   {:label "GUI"
                    :update-fn (fn [] (mouse-position))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (world-mouse-position)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera world-viewport)))
                    :icon (asset "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (gdx/frames-per-second))
                    :icon (asset "images/fps.png")}]})

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
  (let [cam (:camera world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (draw-grid (int left-x) (int bottom-y)
                 (inc (int (:width  world-viewport)))
                 (+ 2 (int (:height world-viewport)))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (draw-filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (draw-filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (draw-filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test []
  (let [position (world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (draw-circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
      (draw-rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (draw-rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (world-mouse-position))
          cell (grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (draw-rectangle x y 1 1
                        (case (:movement @cell)
                          :air  [1 1 0 0.5]
                          :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test)
  (highlight-mouseover-tile))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (draw-rectangle x y (:width entity) (:height entity) color)))

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
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity))
       (catch Throwable t
         (draw-body-rect entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (mouse-on-actor?)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (world-mouse-position)))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(line-of-sight? player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (.bindRoot #'mouseover-eid new-eid)))

(def pausing? true)

(defn- set-paused-flag! []
  (.bindRoot #'paused? (or #_error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @player-eid))
                                (not (or (gdx/key-just-pressed? :p)
                                         (gdx/key-pressed?      :space)))))))

(defn- update-time! []
  (let [delta-ms (min (gdx/delta-time) max-delta)]
    (alter-var-root #'elapsed-time + delta-ms)
    (.bindRoot #'delta-time delta-ms)))

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick potential-field-cache
                               grid
                               faction
                               active-entities
                               max-iterations)))

(defn- tick-entities! []
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
     (error-window! t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! []
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (gdx/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (gdx/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed)))))

(defn- window-controls! []
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (gdx/key-just-pressed? (get window-hotkeys window-id))]
      (ui/toggle-visible! (get (get-actor :windows) window-id))))
  (when (gdx/key-just-pressed? :escape)
    (let [windows (Group/.getChildren (get-actor :windows))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (create-db!)
    (lwjgl/application! (:application config)
                        (proxy [ApplicationAdapter] []
                          (create []
                            (create-asset-manager! (:assets config))
                            (create-graphics! (:graphics config))
                            (ui/load! (:vis-ui config)) ; TODO we don't do dispose!
                            (init-stage!)
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (dispose-asset-manager!)
                            (dispose-graphics!)
                            ; TODO dispose world tiled-map/level resources?
                            )

                          (render []
                            (cache-active-entities!)
                            (set-camera-position! (:position @player-eid))
                            (graphics/clear-screen!)
                            (draw-tiled-map tiled-map
                                            (tile-color-setter raycaster
                                                               explored-tile-corners
                                                               (camera/position (:camera world-viewport))))
                            (draw-on-world-view! (fn []
                                                   (draw-before-entities!)
                                                   (render-entities!)
                                                   (draw-after-entities!)))
                            (.draw stage)
                            (.act stage)
                            (entity/manual-tick (entity/state-obj @player-eid))
                            (update-mouseover-entity!)
                            (set-paused-flag!)
                            (when-not paused?
                              (update-time!)
                              (update-potential-fields!)
                              (tick-entities!))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (remove-destroyed-entities!)

                            (camera-controls!)
                            (window-controls!))

                          (resize [width height]
                            (Viewport/.update ui-viewport    width height true)
                            (Viewport/.update world-viewport width height false))))))
