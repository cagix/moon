(ns cdq.g
  (:require [cdq.animation :as animation]
            [cdq.db :as db]
            [cdq.db.schema :as schema]
            [cdq.db.property :as property]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.grid2d :as g2d]
            cdq.impl.db
            [cdq.interop :as interop]
            [cdq.stage :as stage]
            [cdq.tiled :as tiled]
            [cdq.math :refer [circle->outer-rectangle]]
            [cdq.math.raycaster :as raycaster]
            [cdq.utils :as utils :refer [sort-by-order
                                         tile->middle
                                         pretty-pst
                                         bind-root]]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
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
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector2 MathUtils)
           (com.badlogic.gdx.utils Disposable ScreenUtils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport FitViewport Viewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)))

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

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (graphics/from-sheet ctx/graphics
                           (graphics/sprite-sheet ctx/graphics (ctx/assets file) tilew tileh)
                           [(int (/ sprite-x tilew))
                            (int (/ sprite-y tileh))]))
    (graphics/sprite ctx/graphics (ctx/assets file))))

(defmethod schema/edn->value :s/image [_ edn]
  (edn->sprite edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (animation/create (map edn->sprite frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id]
  (db/build ctx/db property-id))

(defmethod schema/edn->value :s/one-to-many [_ property-ids]
  (set (map #(db/build ctx/db %) property-ids)))

(defn- set-color! [^ShapeDrawer shape-drawer color]
  (.setColor shape-drawer (interop/->color color)))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- draw-text! [{:keys [^BitmapFont font
                           scale
                           batch
                           x
                           y
                           text
                           h-align
                           up?]}]
  (let [data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale (float scale)))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))

(defn- draw-texture-region [^Batch batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn- fit-viewport
  ([width height]
   (fit-viewport width height (OrthographicCamera.)))
  ([width height camera]
   (proxy [FitViewport clojure.lang.ILookup] [width height camera]
     (valAt
       ([key]
        (interop/k->viewport-field this key))
       ([key _not-found]
        (interop/k->viewport-field this key))))))

(defn- ->world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

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

(defn- truetype-font [{:keys [file size quality-scaling]}]
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

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [viewport]
  (let [mouse-x (clamp (.getX Gdx/input)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (.getY Gdx/input)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (let [v2 (Viewport/.unproject viewport (Vector2. mouse-x mouse-y))]
      [(.x v2) (.y v2)])))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} scale world-unit-scale]
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

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defrecord Graphics [^Batch batch
                     ^Texture shape-drawer-texture
                     ^ShapeDrawer shape-drawer
                     cursors
                     default-font
                     world-unit-scale
                     world-viewport
                     get-tiled-map-renderer
                     unit-scale
                     ui-viewport]
  com.badlogic.gdx.utils.Disposable
  (dispose [_]
    (.dispose batch)
    (.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals cursors))
    (Disposable/.dispose default-font))

  graphics/Graphics
  (mouse-position [_]
    ; TODO mapv int needed?
    (mapv int (unproject-mouse-position ui-viewport)))

  (world-mouse-position [_]
    ; TODO clamping only works for gui-viewport ? check. comment if true
    ; TODO ? "Can be negative coordinates, undefined cells."
    (unproject-mouse-position world-viewport))

  (pixels->world-units [_ pixels]
    (* (int pixels) world-unit-scale))

  (draw-image [_ {:keys [texture-region color] :as image} position]
    (draw-texture-region batch
                         texture-region
                         position
                         (unit-dimensions image @unit-scale)
                         0 ; rotation
                         color))

  (draw-rotated-centered [_ {:keys [texture-region color] :as image} rotation [x y]]
    (let [[w h] (unit-dimensions image @unit-scale)]
      (draw-texture-region batch
                           texture-region
                           [(- (float x) (/ (float w) 2))
                            (- (float y) (/ (float h) 2))]
                           [w h]
                           rotation
                           color)))

  (set-camera-position! [_ position]
    (camera/set-position! (:camera world-viewport) position))

  (draw-text [_ {:keys [font scale x y text h-align up?]}]
    (draw-text! {:font (or font default-font)
                 :scale (* (float @unit-scale)
                           (float (or scale 1)))
                 :batch batch
                 :x x
                 :y y
                 :text text
                 :h-align h-align
                 :up? up?}))

  (draw-ellipse [_ [x y] radius-x radius-y color]
    (set-color! shape-drawer color)
    (.ellipse shape-drawer
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (draw-filled-ellipse [_ [x y] radius-x radius-y color]
    (set-color! shape-drawer color)
    (.filledEllipse shape-drawer
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (draw-circle [_ [x y] radius color]
    (set-color! shape-drawer color)
    (.circle shape-drawer
             (float x)
             (float y)
             (float radius)))

  (draw-filled-circle [_ [x y] radius color]
    (set-color! shape-drawer color)
    (.filledCircle shape-drawer
                   (float x)
                   (float y)
                   (float radius)))

  (draw-arc [_ [center-x center-y] radius start-angle degree color]
    (set-color! shape-drawer color)
    (.arc shape-drawer
          (float center-x)
          (float center-y)
          (float radius)
          (float (degree->radians start-angle))
          (float (degree->radians degree))))

  (draw-sector [_ [center-x center-y] radius start-angle degree color]
    (set-color! shape-drawer color)
    (.sector shape-drawer
             (float center-x)
             (float center-y)
             (float radius)
             (float (degree->radians start-angle))
             (float (degree->radians degree))))

  (draw-rectangle [_ x y w h color]
    (set-color! shape-drawer color)
    (.rectangle shape-drawer
                (float x)
                (float y)
                (float w)
                (float h)))

  (draw-filled-rectangle [_ x y w h color]
    (set-color! shape-drawer color)
    (.filledRectangle shape-drawer
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (draw-line [_ [sx sy] [ex ey] color]
    (set-color! shape-drawer color)
    (.line shape-drawer
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [_ width draw-fn]
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth shape-drawer (float old-line-width))))

  (draw-grid [this leftx bottomy gridw gridh cellw cellh color]
    (let [w (* (float gridw) (float cellw))
          h (* (float gridh) (float cellh))
          topy (+ (float bottomy) (float h))
          rightx (+ (float leftx) (float w))]
      (doseq [idx (range (inc (float gridw)))
              :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
        (graphics/draw-line this [linex topy] [linex bottomy] color))
      (doseq [idx (range (inc (float gridh)))
              :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
        (graphics/draw-line this [leftx liney] [rightx liney] color))))

  (draw-on-world-view! [this f]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
    (.begin batch)
    (graphics/with-line-width this world-unit-scale
      (fn []
        ; could pass new 'g' with assoc :unit-scale -> but using ctx/graphics accidentally
        ; -> icon is drawn at too big ! => mutable field.
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (set-cursor! [_ cursor-key]
    (.setCursor Gdx/graphics (utils/safe-get cursors cursor-key)))

  (draw-tiled-map [_ tiled-map color-setter]
    (tiled/draw! (get-tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (resize! [_ width height]
    (Viewport/.update ui-viewport    width height true)
    (Viewport/.update world-viewport width height false))

  (sub-sprite [_ sprite [x y w h]]
    (sprite* (TextureRegion. ^TextureRegion (:texture-region sprite)
                             (int x)
                             (int y)
                             (int w)
                             (int h))
             world-unit-scale))

  (sprite-sheet [_ texture tilew tileh]
    {:image (sprite* (TextureRegion. ^Texture texture) world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (from-sheet [this {:keys [image tilew tileh]} [x y]]
    (graphics/sub-sprite this
                         image
                         [(* x tilew)
                          (* y tileh)
                          tilew
                          tileh]))

  (sprite [_ texture]
    (sprite* (TextureRegion. ^Texture texture) world-unit-scale)))

(defn- create-graphics [{:keys [cursors
                                default-font
                                tile-size
                                world-viewport
                                ui-viewport]}]
  (let [batch (SpriteBatch.)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ tile-size))]
    (map->Graphics
     {:batch batch
      :shape-drawer-texture shape-drawer-texture
      :shape-drawer (ShapeDrawer. batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
      :cursors (utils/mapvals
                (fn [[file [hotspot-x hotspot-y]]]
                  (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                    (.dispose pixmap)
                    cursor))
                cursors)
      :default-font (truetype-font default-font)
      :world-unit-scale  world-unit-scale
      :world-viewport (->world-viewport world-unit-scale world-viewport)
      :get-tiled-map-renderer (memoize (fn [tiled-map]
                                         (tiled/renderer tiled-map
                                                         world-unit-scale
                                                         batch)))
      :ui-viewport (fit-viewport (:width  ui-viewport)
                                 (:height ui-viewport))
      :unit-scale (atom 1)})))

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
    :middle (utils/tile->middle position)
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

(defrecord World [tiled-map
                  grid
                  raycaster
                  content-grid
                  explored-tile-corners
                  entity-ids
                  potential-field-cache
                  active-entities]
  world/World
  (cell [_ position]
    ; assert/document integer ?
    (grid position)))

(defn- create-world [{:keys [tiled-map start-position]}]
  (let [width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)
        grid (create-grid tiled-map)]
    (map->World {:tiled-map tiled-map
                 :start-position start-position
                 :grid grid
                 :raycaster (create-raycaster grid)
                 :content-grid (content-grid/create {:cell-size 16
                                                     :width  width
                                                     :height height})
                 :explored-tile-corners (atom (g2d/create-grid width
                                                               height
                                                               (constantly false)))
                 :entity-ids (atom {})
                 :potential-field-cache (atom nil)
                 :active-entities nil})))

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
  (bind-root #'ctx/world (create-world ((requiring-resolve world-fn))))
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
    (bind-root #'ctx/db (cdq.impl.db/create))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource "moon.png")))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (bind-root #'ctx/assets (load-assets))
                            (bind-root #'ctx/graphics (create-graphics (:graphics config)))
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
