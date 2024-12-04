(ns forge.core
  ; ==
  ; * library - private stuff set public, e.g. sprite-batch
  ; => can use parts also ! (because small functions r there )
  ; => can make really short cool examples
  ; => colors have to be highlighted, check also that vim plugin maybe
  ; => check private names also for collisions
  ; => which functions depend on which context can highlight/sort like that
  ; => world somewhere else? different deps? shouldn't dep on forge.core ?!
  ; but world already in there!
  ; * framework (if using existing app components)
  ; * language
  ; * not a wrapper -> takes ApplicationAdapter!

  ; ==> worl shouldn't be in here because it has not those gdx dependencies
  ; it shoud depend on forge.core ... ?
  ; db maybe also does not need to be there.....
  ; db is actually a separate part with malli dependencies ?!!?!
  ; 1. level - forge.core then
  ; e.g. editor does not depend on world stuff ---
  ; -> where do you draw the line ?
  ; or multiple namespaces.........
  ; e.g. rand could be moved smw else completely ......

  ; => makes sense maybe in here only till 'components-app' ?

  ; => Can I make and build mayself cool
  ; apps now?
  ; e.g. just world-view & tiledmap-renderer & spritebatch
  ; no shape-drawer?!

  ; but then there can also exist a library without state?
  ; with just 'sprite-batch'/'asset-manager'/... and 'key-pressed?' and stuff?
  ; I can do multiple :refer :all?

  ; => Layer your dependencies , context namespace
  ; a bit more coarse but still
  ; e.g. whole gdx stuff and malli/world totally independent ..

  ; 1. level : bind-root, defsystem(?) ... ?

  (:require [clj-commons.pretty.repl :as pretty-repl] ; and
             ; shoud
            [data.grid2d :as g2d] ; this
            [forge.base :refer :all]
            [forge.db :as db] ; this
            [malli.core :as m] ; this
            [reduce-fsm :as fsm]) ; this
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Camera Color Colors Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont Batch TextureRegion SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.scenes.scene2d Actor Stage Touchable Group)
           (com.badlogic.gdx.scenes.scene2d.ui Cell Widget Image Label Button Table WidgetGroup Stack ButtonGroup HorizontalGroup VerticalGroup Window Tree$Node)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable Drawable)
           (com.badlogic.gdx.maps MapLayer MapLayers MapProperties)
           (com.badlogic.gdx.maps.tiled TmxMapLoader TiledMap TiledMapTile TiledMapTileLayer TiledMapTileLayer$Cell)
           (com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile)
           (com.badlogic.gdx.math MathUtils Circle Intersector Rectangle Vector2 Vector3)
           (com.badlogic.gdx.utils Align Scaling Disposable ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip VisTextButton VisCheckBox VisSelectBox VisImage VisImageButton VisTextField VisWindow VisTable VisLabel VisSplitPane VisScrollPane Separator VisTree)
           (forge OrthogonalTiledMapRenderer ColorSetter RayCaster)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn freetype-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (.internal Gdx/files file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- gdx-static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str-replace (str-upper-case (name k)) "-" "_")))))

(defn equal? [a b]
  (MathUtils/isEqual a b))

(defn clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn gdx-align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn gdx-scaling [k]
  (case k
    :fill Scaling/fill))

(defn gdx-color
  ([r g b]
   (gdx-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(def ^Color black Color/BLACK)
(def ^Color white Color/WHITE)

(defn ->gdx-color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (gdx-static-field "graphics.Color" c)
        (vector? c) (apply gdx-color c)
        :else (throw (ex-info "Cannot understand color" c))))

(def ^:private k->input-button (partial gdx-static-field "Input$Buttons"))
(def ^:private k->input-key    (partial gdx-static-field "Input$Keys"))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn exit-app []
  (.exit Gdx/app))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(defn set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (FileHandle/.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- asset-manager ^AssetManager []
  (proxy [AssetManager clojure.lang.ILookup] []
    (valAt [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn- load-assets [folder]
  (let [manager (asset-manager)]
    (doseq [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                          [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
            file (map #(str-replace-first % folder "")
                      (recursively-search (internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

; we need a base 'language' without dependencies of gdx
;if vector -> first, if keyword -> kw, else -> call Disposable protocol
; one language for 'verbs' - create/resize/etc....
; maybe for accessing stuff like fps we can do it with keywords
; access gdx context (:frames-per-second gdx)  ?
(def dispose Disposable/.dispose)

(defn- check-cleanup-visui! []
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose)))

(defn- font-enable-markup! []
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true)))

(defn- set-tooltip-config! []
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  )

(defmethods :app/vis-ui
  (app-create [[_ skin-scale]]
    (check-cleanup-visui!)
    (VisUI/load (case skin-scale
                  :skin-scale/x1 VisUI$SkinScale/X1
                  :skin-scale/x2 VisUI$SkinScale/X2))
    (font-enable-markup!)
    (set-tooltip-config!))
  (app-dispose [_]
    (VisUI/dispose)))

(defmethods :app/assets
  (app-create [[_ folder]]
    (def assets (load-assets folder)))
  (app-dispose [_]
    (dispose assets)))

(defmethods :app/sprite-batch
  (app-create [_]
    (def batch (SpriteBatch.)))
  (app-dispose [_]
    (dispose batch)))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(let [pixel-texture (atom nil)]
  (defmethods :app/shape-drawer
    (app-create [_]
      (reset! pixel-texture (white-pixel-texture))
      (def shape-drawer (ShapeDrawer. batch (TextureRegion. ^Texture @pixel-texture 1 0 1 1))))
    (app-dispose [_]
      (dispose @pixel-texture))))

(defmethods :app/default-font
  (app-create [[_ font]]
    (def default-font (freetype-font font)))
  (app-dispose [_]
    (dispose default-font)))

(defmethods :app/cursors
  (app-create [[_ data]]
    (def cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                    (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                          cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                      (dispose pixmap)
                                      cursor))
                                  data)))
  (app-dispose [_]
    (run! dispose (vals cursors))))

(defmethods :app/gui-viewport
  (app-create [[_ [width height]]]
    (def gui-viewport-width  width)
    (def gui-viewport-height height)
    (def gui-viewport (FitViewport. width height (OrthographicCamera.))))
  (app-resize [_ w h]
    (.update gui-viewport w h true)))

(defmethods :app/world-viewport
  (app-create [[_ [width height tile-size]]]
    (def world-unit-scale (float (/ tile-size)))
    (def world-viewport-width  width)
    (def world-viewport-height height)
    (def world-viewport (let [world-width  (* width  world-unit-scale)
                              world-height (* height world-unit-scale)
                              camera (OrthographicCamera.)
                              y-down? false]
                          (.setToOrtho camera y-down? world-width world-height)
                          (FitViewport. world-width world-height camera))))
  (app-resize [_ w h]
    (.update world-viewport w h true)))

(defmethods :app/cached-map-renderer
  (app-create [_]
    (def cached-map-renderer
      (memoize
       (fn [tiled-map]
         (OrthogonalTiledMapRenderer. tiled-map
                                      (float world-unit-scale)
                                      batch))))))

(defrecord StageScreen [^Stage stage sub-screen]
  Screen
  (screen-enter [_]
    (.setInputProcessor Gdx/input stage)
    (screen-enter sub-screen))

  (screen-exit [_]
    (.setInputProcessor Gdx/input nil)
    (screen-exit sub-screen))

  (screen-render [_]
    (.act stage)
    (screen-render sub-screen)
    (.draw stage))

  (screen-destroy [_]
    (.dispose stage)
    (screen-destroy sub-screen)))

(defn children
  "Returns an ordered list of child actors in this group."
  [^Group group]
  (seq (.getChildren group)))

(defn clear-children!
  "Removes all actors from this group and unfocuses them."
  [^Group group]
  (.clearChildren group))

(defn add-actor!
  "Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this group, no changes are made."
  [^Group group actor]
  (.addActor group actor))

(defn find-actor-with-id [group id]
  (let [actors (children group)
        ids (keep Actor/.getUserObject actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (Actor/.getUserObject %)) actors))))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [Stage clojure.lang.ILookup] [gui-viewport batch]
                (valAt
                  ([id]
                   (find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defmethods :app/screens
  (app-create [[_ {:keys [ks first-k]}]]
    (bind-root #'screens (mapvals stage-screen (mapvals
                                                (fn [ns-sym]
                                                  (require ns-sym)
                                                  ((ns-resolve ns-sym 'create)))
                                                ks)))
    (change-screen first-k))
  (app-dispose [_]
    (run! screen-destroy (vals screens)))
  (app-render [_]
    (ScreenUtils/clear black)
    (screen-render (current-screen))))

(defn add-actor [actor]
  (.addActor (screen-stage) actor))

(defn reset-stage [new-actors]
  (.clear (screen-stage))
  (run! add-actor new-actors))

(defn play-sound [name]
  (Sound/.play (get assets (str "sounds/" name ".wav"))))

(def grid2d g2d/create-grid)

(extend-type Actor
  HasVisible
  (set-visible [actor bool]
    (.setVisible actor bool))
  (visible? [actor]
    (.isVisible actor)))

(extend-type TiledMapTileLayer
  HasVisible
  (set-visible [layer bool]
    (.setVisible layer bool))
  (visible? [layer]
    (.isVisible layer)))

(defn- m-v2
  (^Vector2 [[x y]] (Vector2. x y))
  (^Vector2 [x y]   (Vector2. x y)))

(defn- ->p [^Vector2 v]
  [(.x v) (.y v)])

(defn v-scale [v n]
  (->p (.scl (m-v2 v) (float n))))

(defn v-normalise [v]
  (->p (.nor (m-v2 v))))

(defn v-add [v1 v2]
  (->p (.add (m-v2 v1) (m-v2 v2))))

(defn v-length [v]
  (.len (m-v2 v)))

(defn v-distance [v1 v2]
  (.dst (m-v2 v1) (m-v2 v2)))

(defn v-normalised? [v]
  (equal? 1 (v-length v)))

(defn v-direction [[sx sy] [tx ty]]
  (v-normalise [(- (float tx) (float sx))
                (- (float ty) (float sy))]))

(defn v-angle-from-vector
  "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), counterclockwise"
  [v]
  (.angleDeg (m-v2 v) (Vector2. 0 1)))

(comment

 (pprint
  (for [v [[0 1]
           [1 1]
           [1 0]
           [1 -1]
           [0 -1]
           [-1 -1]
           [-1 0]
           [-1 1]]]
    [v
     (.angleDeg (m-v2 v) (Vector2. 0 1))
     (get-angle-from-vector (m-v2 v))]))

 )

(defn- m->shape [m]
  (cond
   (rectangle? m) (let [{:keys [left-bottom width height]} m
                        [x y] left-bottom]
                    (Rectangle. x y width height))

   (circle? m) (let [{:keys [position radius]} m
                     [x y] position]
                 (Circle. x y radius))

   :else (throw (Error. (str m)))))

(defmulti ^:private overlaps?* (fn [a b] [(class a) (class b)]))

(defmethod overlaps?* [Circle Circle]
  [^Circle a ^Circle b]
  (Intersector/overlaps a b))

(defmethod overlaps?* [Rectangle Rectangle]
  [^Rectangle a ^Rectangle b]
  (Intersector/overlaps a b))

(defmethod overlaps?* [Rectangle Circle]
  [^Rectangle rect ^Circle circle]
  (Intersector/overlaps circle rect))

(defmethod overlaps?* [Circle Rectangle]
  [^Circle circle ^Rectangle rect]
  (Intersector/overlaps circle rect))

(defn overlaps? [a b]
  (overlaps?* (m->shape a) (m->shape b)))

(defn rect-contains? [rectangle [x y]]
  (Rectangle/.contains (m->shape rectangle) x y))

(def val-max-schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(defmethod db/malli-form :s/val-max [_]
  (m/form val-max-schema))

(defn val-max-ratio
  "If mx and v is 0, returns 0, otherwise (/ v mx)"
  [[^int v ^int mx]]
  {:pre [(m/validate val-max-schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))

(declare start-world)

(defn load-tmx-map
  "Has to be disposed."
  [file]
  (.load (TmxMapLoader.) file))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- texture-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} world-unit-scale scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defn- sprite* [world-unit-scale texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions world-unit-scale 1) ; = scale 1
      map->Sprite))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str-split #"\n")
      count
      (* (.getLineHeight font))))

(defn add-color [name-str color]
  (Colors/put name-str (->gdx-color color)))

(defn- draw-texture-region [texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w ; width height
         h
         1 ; scaling factor
         1
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn- sd-color [color]
  (.setColor shape-drawer (->gdx-color color)))

(defn draw-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn draw-filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn draw-circle [[x y] radius color]
  (sd-color color)
  (.circle shape-drawer (float x) (float y) (float radius)))

(defn draw-filled-circle [[x y] radius color]
  (sd-color color)
  (.filledCircle shape-drawer (float x) (float y) (float radius)))

(defn draw-arc [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.arc shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn draw-sector [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.sector shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn draw-rectangle [x y w h color]
  (sd-color color)
  (.rectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn draw-filled-rectangle [x y w h color]
  (sd-color color)
  (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn draw-line [[sx sy] [ex ey] color]
  (sd-color color)
  (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (sd-color color)
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw-line shape-drawer [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw-line shape-drawer [leftx liney] [rightx liney]))))

(defn with-line-width [width draw-fn]
  (let [old-line-width (.getDefaultLineWidth shape-drawer)]
    (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth shape-drawer (float old-line-width))))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [^Viewport viewport]
  (let [mouse-x (clamp (.getX Gdx/input)
                       (.getLeftGutterWidth viewport)
                       (.getRightGutterX viewport))
        mouse-y (clamp (.getY Gdx/input)
                       (.getTopGutterHeight viewport)
                       (.getTopGutterY viewport))
        coords (.unproject viewport (Vector2. mouse-x mouse-y))]
    [(.x coords) (.y coords)]))

(defn gui-mouse-position []
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position gui-viewport)))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn world-camera []
  (.getCamera world-viewport))

(defn ->texture-region
  ([path]
   (TextureRegion. ^Texture (get assets path)))
  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))

(defn ->image [path]
  (sprite* world-unit-scale
           (->texture-region path)))

(defn sub-image [image bounds]
  (sprite* world-unit-scale
           (->texture-region (:texture-region image) bounds)))

(defn sprite-sheet [path tilew tileh]
  {:image (->image path)
   :tilew tilew
   :tileh tileh})

(defn ->sprite [{:keys [image tilew tileh]} [x y]]
  (sub-image image
             [(* x tilew) (* y tileh) tilew tileh]))

(defn draw-image [{:keys [texture-region color] :as image} position]
  (draw-texture-region texture-region
                       position
                       (unit-dimensions image *unit-scale*)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image *unit-scale*)]
    (draw-texture-region texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- draw-with [^Viewport viewport unit-scale draw-fn]
  (.setColor batch white) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined (.getCamera viewport)))
  (.begin batch)
  (with-line-width unit-scale
    #(binding [*unit-scale* unit-scale]
       (draw-fn)))
  (.end batch))

(defn draw-on-world-view [render-fn]
  (draw-with world-viewport
             world-unit-scale
             render-fn))

(defn edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (->sprite (sprite-sheet file tilew tileh)
                [(int (/ sprite-x tilew))
                 (int (/ sprite-y tileh))]))
    (->image file)))

(defmethod db/edn->value :s/image [_ edn]
  (edn->image edn))

(defn mouse-on-actor? []
  (let [[x y] (gui-mouse-position)]
    (.hit (screen-stage) x y true)))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font x y text h-align up? scale]}]
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))]
    (.setScale data (* old-scale
                       (float *unit-scale*)
                       (float (or scale 1))))
    (.draw font
           batch
           (str text)
           (float x)
           (+ (float y) (float (if up? (text-height font text) 0)))
           (float 0) ; target-width
           (gdx-align (or h-align :center))
           false) ; wrap false, no need target-width
    (.setScale data old-scale)))

(defn set-cursor [cursor-key]
  (.setCursor Gdx/graphics (safe-get cursors cursor-key)))

(defn layer-name ^String [layer]
  (if (keyword? layer)
    (name layer)
    (.getName ^MapLayer layer)))

(defn- props-lookup [has-properties key]
  (.get (m-props has-properties) (name key)))

(comment
 ; could do this but slow -> fetch directly necessary properties
 (defn properties [obj]
   (let [^MapProperties ps (.getProperties obj)]
     (zipmap (map keyword (.getKeys ps)) (.getValues ps))))
 )

(extend-protocol HasProperties
  TiledMap
  (m-props [tiled-map] (.getProperties tiled-map))
  (get-property [tiled-map key] (props-lookup tiled-map key))

  MapLayer
  (m-props [layer] (.getProperties layer))
  (get-property [layer key] (props-lookup layer key))

  TiledMapTile
  (m-props [tile] (.getProperties tile))
  (get-property [tile key] (props-lookup tile key)))

(defn tm-width  [tiled-map] (get-property tiled-map :width))
(defn tm-height [tiled-map] (get-property tiled-map :height))

(defn layers ^MapLayers [tiled-map]
  (TiledMap/.getLayers tiled-map))

(defn layer-index
  "Returns nil or the integer index of the layer.
  Layer can be keyword or an instance of TiledMapTileLayer."
  [tiled-map layer]
  (let [idx (.getIndex (layers tiled-map) (layer-name layer))]
    (when-not (= idx -1)
      idx)))

(defn get-layer
  "Returns the layer with name (string)."
  [tiled-map layer-name]
  (.get (layers tiled-map) ^String layer-name))

(defn remove-layer!
  "Removes the layer, layer can be keyword or an actual layer object."
  [tiled-map layer]
  (.remove (layers tiled-map)
           (int (layer-index tiled-map layer))))

(defn cell-at
  "Layer can be keyword or layer object.
  Position vector [x y].
  If the layer is part of tiledmap, returns the TiledMapTileLayer$Cell at position."
  [tiled-map layer [x y]]
  (when-let [layer (get-layer tiled-map (layer-name layer))]
    (.getCell ^TiledMapTileLayer layer x y)))

(defn property-value
  "Returns the property value of the tile at the cell in layer.
  If there is no cell at this position in the layer returns :no-cell.
  If the property value is undefined returns :undefined.
  Layer is keyword or layer object."
  [tiled-map layer position property-key]
  (assert (keyword? property-key))
  (if-let [cell (cell-at tiled-map layer position)]
    (if-let [value (get-property (.getTile ^TiledMapTileLayer$Cell cell) property-key)]
      value
      :undefined)
    :no-cell))

(defn- map-positions
  "Returns a sequence of all [x y] positions in the tiledmap."
  [tiled-map]
  (for [x (range (tm-width  tiled-map))
        y (range (tm-height tiled-map))]
    [x y]))

(defn positions-with-property
  "If the layer (keyword or layer object) does not exist returns nil.
  Otherwise returns a sequence of [[x y] value] for all tiles who have property-key."
  [tiled-map layer property-key]
  (when (layer-index tiled-map layer)
    (for [position (map-positions tiled-map)
          :let [[x y] position
                value (property-value tiled-map layer position property-key)]
          :when (not (#{:undefined :no-cell} value))]
      [position value])))

(def copy-tile
  "Memoized function.
  Tiles are usually shared by multiple cells.
  https://libgdx.com/wiki/graphics/2d/tile-maps#cells
  No copied-tile for AnimatedTiledMapTile yet (there was no copy constructor/method)"
  (memoize
   (fn [^StaticTiledMapTile tile]
     (assert tile)
     (StaticTiledMapTile. tile))))

(defn static-tiled-map-tile [texture-region]
  (assert texture-region)
  (StaticTiledMapTile. ^TextureRegion texture-region))

(defn set-tile! [^TiledMapTileLayer layer [x y] tile]
  (let [cell (TiledMapTileLayer$Cell.)]
    (.setTile cell tile)
    (.setCell layer x y cell)))

(defn cell->tile [cell]
  (.getTile ^TiledMapTileLayer$Cell cell))

(defn add-layer! [tiled-map & {:keys [name visible properties]}]
  (let [layer (TiledMapTileLayer. (tm-width  tiled-map)
                                  (tm-height tiled-map)
                                  (get-property tiled-map :tilewidth)
                                  (get-property tiled-map :tileheight))]
    (.setName layer name)
    (when properties
      (.putAll ^MapProperties (m-props layer) properties))
    (.setVisible layer visible)
    (.add ^MapLayers (layers tiled-map) layer)
    layer))

(defn empty-tiled-map []
  (TiledMap.))

(defn put! [^MapProperties properties key value]
  (.put properties key value))

(defn put-all! [^MapProperties properties other-properties]
  (.putAll properties other-properties))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (let [^OrthogonalTiledMapRenderer map-renderer (cached-map-renderer tiled-map)]
    (.setColorSetter map-renderer (reify ColorSetter
                                    (apply [_ color x y]
                                      (color-setter color x y))))
    (.setView map-renderer (world-camera))
    (->> tiled-map
         layers
         (filter visible?)
         (map (partial layer-index tiled-map))
         int-array
         (.render map-renderer))))

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn- set-center [^Actor actor x y]
  (.setPosition actor
                (- x (/ (.getWidth  actor) 2))
                (- y (/ (.getHeight actor) 2))))

(defn actor-hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn- set-cell-opts [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int   arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg))
      :width      (.width     cell (float arg))
      :height     (.height    cell (float arg))
      :center?    (.center    cell)
      :right?     (.right     cell)
      :left?      (.left      cell))))

(defn add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (.add table ^Actor (:actor props-or-actor))
                                 (set-cell-opts (dissoc props-or-actor :actor)))
       :else (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn- set-table-opts [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts (.defaults table) cell-defaults)
  (add-rows! table rows))

(defn horizontal-separator-cell [colspan]
  {:actor (Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (Separator. "vertical")
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn- set-actor-opts [^Actor a {:keys [id name visible? touchable center-position position] :as opts}]
  (when id                          (.setUserObject        a id))
  (when name                        (.setName      a name))
  (when (contains? opts :visible?)  (.setVisible   a (boolean visible?)))
  (when touchable                   (.setTouchable a (case touchable
                                                       :children-only Touchable/childrenOnly
                                                       :disabled      Touchable/disabled
                                                       :enabled       Touchable/enabled)))
  (when-let [[x y] center-position] (set-center    a x y))
  (when-let [[x y] position]        (.setPosition  a x y))
  a)

(defn- set-opts [actor opts]
  (set-actor-opts actor opts)
  (when (instance? Table actor)
    (set-table-opts actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  actor)

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group."
  [class args]
  `(proxy [~class clojure.lang.ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(defn ui-group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup Group [])]
    (run! #(add-actor! group %) actors)
    (set-opts group opts)))

(defn horizontal-group ^HorizontalGroup [{:keys [space pad]}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    group))

(defn vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(add-actor! group %) actors)
    group))

(defn add-tooltip!
  "tooltip-text is a (fn []) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [^Actor a tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (.setText this (str (tooltip-text))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip ^Actor a)
    (.setContent tooltip ^Actor label))
  a)

(defn remove-tooltip! [^Actor a]
  (Tooltip/removeTooltip a))

(defn button-group [{:keys [max-check-count min-check-count]}]
  (let [bg (ButtonGroup.)]
    (.setMaxCheckCount bg max-check-count)
    (.setMinCheckCount bg min-check-count)
    bg))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (VisCheckBox. ^String text)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(defn ui-table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts opts)))

(defn ui-window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts opts)))

(defn label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn text-field [^String text opts]
  (-> (VisTextField. text)
      (set-opts opts)))

(defn ui-stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defmulti ^:private ->vis-image type)
(defmethod ->vis-image Drawable      [^Drawable d      ] (VisImage.  d))
(defmethod ->vis-image TextureRegion [^TextureRegion tr] (VisImage. tr))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (->vis-image object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts opts)))

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(defn texture-region-drawable [^TextureRegion texture-region]
  (TextureRegionDrawable. texture-region))

(defn scroll-pane [actor]
  (let [scroll-pane (VisScrollPane. actor)]
    (Actor/.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (.getParent actor)
           (button-class? (.getParent actor)))))

(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (.getParent actor)]
      (when-let [p (.getParent p)]
        (and (instance? VisWindow p)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [^Actor actor]
  (if-let [p (.getParent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [^Actor actor]
  (.pack (find-ancestor-window actor)))

(declare ^:dynamic *on-clicked-actor*)

(defn change-listener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (VisTextButton. ^String text)]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  (^VisImageButton [image on-clicked]
   (image-button image on-clicked {}))
  (^VisImageButton [{:keys [^TextureRegion texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (TextureRegionDrawable. ^TextureRegion texture-region)
         button (VisImageButton. drawable)]
     (when scale
       (let [[w h] [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
         (.setMinSize drawable (float (* scale w)) (float (* scale h)))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn ui-actor ^Actor [{:keys [draw act]}]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when draw (draw)))
    (act [_delta]
      (when act (act)))))

(defn ui-widget [draw!]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (draw! this))))

(defn set-drawable! [^Image image drawable]
  (.setDrawable image drawable))

(defn set-min-size! [^TextureRegionDrawable drawable size]
  (.setMinSize drawable (float size) (float size)))

(defn tinted-drawable
  "Creates a new drawable that renders the same as this drawable tinted the specified color."
  [drawable color]
  (.tint ^TextureRegionDrawable drawable color))

(defn ui-tree []
  (VisTree.))

(defn t-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn background-image []
  (image->widget (->image "images/moon_background.png")
                 {:fill-parent? true
                  :scaling :fill
                  :align :center}))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (screen-stage))))
  (add-actor
   (ui-window {:title title
               :rows [[(label text)]
                      [(text-button button-text
                                    (fn []
                                      (Actor/.remove (::modal (screen-stage)))
                                      (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ gui-viewport-width 2)
                                 (* gui-viewport-height (/ 3 4))]
               :pack? true})))

(defn pretty-pst [t]
  (binding [*print-level* 3]
    (pretty-repl/pretty-pst t 24)))

(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor
   (ui-window {:title "Error"
               :rows [[(label (binding [*print-level* 3]
                                (with-err-str
                                  (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))

(def ^:private player-message-duration-seconds 1.5)

(def ^:private message-to-player nil)

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (draw-text {:x (/ gui-viewport-width 2)
                :y (+ (/ gui-viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (delta-time))
    (when (>= counter player-message-duration-seconds)
      (bind-root #'message-to-player nil))))

(defn player-message-actor []
  (ui-actor {:draw draw-player-message
             :act check-remove-message}))

(defn player-message-show [message]
  (bind-root #'message-to-player {:message message :counter 0}))

(defn cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn valid-slot? [[slot _] item]
  (or (= :inventory.slot/bag slot)
      (= (:item/slot item) slot)))

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; this is not required but can be asserted, all of one name should have count if others have count
       (= (:property/id item-a) (:property/id item-b))))

(defn cam-position
  "Returns camera position as [x y] vector."
  [^Camera camera]
  [(.x (.position camera))
   (.y (.position camera))])

(defn set-position!
  "Sets x and y and calls update on the camera."
  [^Camera camera [x y]]
  (set! (.x (.position camera)) (float x))
  (set! (.y (.position camera)) (float y))
  (.update camera))

(defn frustum [^Camera camera]
  (let [frustum-points (for [^Vector3 point (take 4 (.planePoints (.frustum camera)))
                             :let [x (.x point)
                                   y (.y point)]]
                         [x y])
        left-x   (apply min (map first  frustum-points))
        right-x  (apply max (map first  frustum-points))
        bottom-y (apply min (map second frustum-points))
        top-y    (apply max (map second frustum-points))]
    [left-x right-x bottom-y top-y]))

(defn visible-tiles [camera]
  (let [[left-x right-x bottom-y top-y] (frustum camera)]
    (for  [x (range (int left-x)   (int right-x))
           y (range (int bottom-y) (+ 2 (int top-y)))]
      [x y])))

(defn calculate-zoom
  "calculates the zoom value for camera to see all the 4 points."
  [^Camera camera & {:keys [left top right bottom]}]
  (let [viewport-width  (.viewportWidth  camera)
        viewport-height (.viewportHeight camera)
        [px py] (cam-position camera)
        px (float px)
        py (float py)
        leftx (float (left 0))
        rightx (float (right 0))
        x-diff (max (- px leftx) (- rightx px))
        topy (float (top 1))
        bottomy (float (bottom 1))
        y-diff (max (- topy py) (- py bottomy))
        vp-ratio-w (/ (* x-diff 2) viewport-width)
        vp-ratio-h (/ (* y-diff 2) viewport-height)
        new-zoom (max vp-ratio-w vp-ratio-h)]
    new-zoom))

(defn zoom [^OrthographicCamera camera]
  (.zoom camera))

(defn set-zoom!
  "Sets the zoom value and updates."
  [^OrthographicCamera camera amount]
  (set! (.zoom camera) amount)
  (.update camera))

(defn reset-zoom!
  "Sets the zoom value to 1."
  [camera]
  (set-zoom! camera 1))

(defmulti generate-level* (fn [world] (:world/generator world)))

(defn generate-level [world-props]
  (assoc (generate-level* world-props)
         :world/player-creature
         (:world/player-creature world-props)))

; TODO performance bottleneck -> every time getting same layers
; takes 600 ms to read movement-properties
; lazy seqs??

(defn- tile-movement-property [tiled-map layer position]
  (let [value (property-value tiled-map layer position :movement)]
    (assert (not= value :undefined)
            (str "Value for :movement at position "
                 position  " / mapeditor inverted position: " [(position 0)
                                                               (- (dec (tm-height tiled-map))
                                                                  (position 1))]
                 " and layer " (layer-name layer) " is undefined."))
    (when-not (= :no-cell value)
      value)))

(defn- movement-property-layers [tiled-map]
  (filter #(get-property % :movement-properties)
          (reverse
           (layers tiled-map))))

(defn movement-properties [tiled-map position]
  (for [layer (movement-property-layers tiled-map)]
    [(layer-name layer)
     (tile-movement-property tiled-map layer position)]))

(defn movement-property [tiled-map position]
  (or (->> tiled-map
           movement-property-layers
           (some #(tile-movement-property tiled-map % position)))
      "none"))

(defn- content-grid-create [{:keys [cell-size width height]}]
  {:grid (grid2d (inc (int (/ width  cell-size))) ; inc because corners
                 (inc (int (/ height cell-size)))
                 (fn [idx]
                   (atom {:idx idx,
                          :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defn- content-grid-update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{::keys [content-cell] :as entity} @eid
        [x y] (:position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc ::content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(defn- content-grid-remove-entity! [eid]
  (-> @eid
      ::content-cell
      (swap! update :entities disj eid)))

(defn- content-grid-active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     ::content-cell
                     deref
                     :idx)]
         (cons idx (get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(declare world-tiled-map
         explored-tile-corners
         world-grid
         tick-error
         paused?
         ids->eids
         content-grid
         ^:private ray-caster
         ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed-time
         ^{:doc "The game logic update delta-time. Different then forge.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         world-delta
         player-eid)

; boolean array used because 10x faster than access to clojure grid data structure

; this was a serious performance bottleneck -> alength is counting the whole array?
;(def ^:private width alength)
;(def ^:private height (comp alength first))

; does not show warning on reflection, but shows cast-double a lot.
(defn- rc-blocked? [[arr width height] [start-x start-y] [target-x target-y]]
  (RayCaster/rayBlocked (double start-x)
                        (double start-y)
                        (double target-x)
                        (double target-y)
                        width ;(width boolean-2d-array)
                        height ;(height boolean-2d-array)
                        arr))

#_(defn ray-steplist [boolean-2d-array [start-x start-y] [target-x target-y]]
  (seq
   (RayCaster/castSteplist start-x
                           start-y
                           target-x
                           target-y
                           (width boolean-2d-array)
                           (height boolean-2d-array)
                           boolean-2d-array)))

#_(defn ray-maxsteps [boolean-2d-array [start-x start-y] [vector-x vector-y] max-steps]
  (let [erg (RayCaster/castMaxSteps start-x
                                    start-y
                                    vector-x
                                    vector-y
                                    (width boolean-2d-array)
                                    (height boolean-2d-array)
                                    boolean-2d-array
                                    max-steps
                                    max-steps)]
    (if (= -1 erg)
      :not-blocked
      erg)))

; STEPLIST TEST

#_(def current-steplist (atom nil))

#_(defn steplist-contains? [tilex tiley] ; use vector equality
  (some
    (fn [[x y]]
      (and (= x tilex) (= y tiley)))
    @current-steplist))

#_(defn render-line-middle-to-mouse [color]
  (let [[x y] (input/get-mouse-pos)]
    (g/draw-line (/ (g/viewport-width) 2)
                 (/ (g/viewport-height) 2)
                 x y color)))

#_(defn update-test-raycast-steplist []
    (reset! current-steplist
            (map
             (fn [step]
               [(.x step) (.y step)])
             (raycaster/ray-steplist (get-cell-blocked-boolean-array)
                                     (:position @world-player)
                                     (g/map-coords)))))

;; MAXSTEPS TEST

#_(def current-steps (atom nil))

#_(defn update-test-raycast-maxsteps []
    (let [maxsteps 10]
      (reset! current-steps
              (raycaster/ray-maxsteps (get-cell-blocked-boolean-array)
                                      (v-direction (g/map-coords) start)
                                      maxsteps))))

#_(defn draw-test-raycast []
  (let [start (:position @world-player)
        target (g/map-coords)
        color (if (fast-ray-blocked? start target) g/red g/green)]
    (render-line-middle-to-mouse color)))

; PATH BLOCKED TEST

#_(defn draw-test-path-blocked [] ; TODO draw in map no need for screenpos-of-tilepos
  (let [[start-x start-y] (:position @world-player)
        [target-x target-y] (g/map-coords)
        [start1 target1 start2 target2] (create-double-ray-endpositions start-x start-y target-x target-y 0.4)
        [start1screenx,start1screeny]   (screenpos-of-tilepos start1)
        [target1screenx,target1screeny] (screenpos-of-tilepos target1)
        [start2screenx,start2screeny]   (screenpos-of-tilepos start2)
        [target2screenx,target2screeny] (screenpos-of-tilepos target2)
        color (if (is-path-blocked? start1 target1 start2 target2)
                g/red
                g/green)]
    (g/draw-line start1screenx start1screeny target1screenx target1screeny color)
    (g/draw-line start2screenx start2screeny target2screenx target2screeny color)))

; TO math.... // not tested
(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v-direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v-normal-vectors v)
        normal1 (v-scale normal1 (/ path-w 2))
        normal2 (v-scale normal2 (/ path-w 2))
        start1  (v-add [start-x  start-y]  normal1)
        start2  (v-add [start-x  start-y]  normal2)
        target1 (v-add [target-x target-y] normal1)
        target2 (v-add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn- path-blocked?*
  "path-w in tiles. casts two rays."
  [raycaster start target path-w]
  (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
    (or
     (rc-blocked? raycaster start1 target1)
     (rc-blocked? raycaster start2 target2))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- init-raycaster [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    (bind-root #'ray-caster [arr width height])))

(defn ray-blocked? [start target]
  (rc-blocked? ray-caster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (path-blocked?* ray-caster start target path-w))

(def mouseover-eid nil)

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

(defn timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn reset-timer [{:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed-time duration)))

(defn finished-ratio [{:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(defn world-clear [] ; responsibility of screen? we are not creating the tiled-map here ...
  (when (bound? #'world-tiled-map)
    (dispose world-tiled-map)))

(defn cell [position]
  (get world-grid position))

(defn rectangle->cells [rectangle]
  (into [] (keep world-grid) (rectangle->tiles rectangle)))

(defn circle->cells [circle]
  (->> circle
       circle->outer-rectangle
       rectangle->cells))

(defn cells->entities [cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [circle]
  (->> (circle->cells circle)
       (map deref)
       cells->entities
       (filter #(overlaps? circle @%))))

(defn- set-cells! [eid]
  (let [cells (rectangle->cells @eid)]
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
(defn- rectangle->occupied-cells [{:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (rectangle->cells rectangle)
    [(get world-grid
          [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
           (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [eid]
  (let [cells (rectangle->occupied-cells @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn cached-adjacent-cells [cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (into [] (keep world-grid) (-> @cell :position get-8-neighbour-positions))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn point->entities [position]
  (when-let [cell (get world-grid (->tile position))]
    (filter #(rect-contains? @% position)
            (:entities @cell))))

(defn- grid-add-entity [eid]
  (set-cells! eid)
  (when (:collides? @eid)
    (set-occupied-cells! eid)))

(defn- grid-remove-entity [eid]
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn- grid-entity-position-changed [eid]
  (remove-from-cells! eid)
  (set-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! eid)))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  GridCell
  (cell-blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (draw-rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t))))

(defn- add-to-world [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'ids->eids assoc id eid))
  (content-grid-update-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (grid-add-entity eid))

(defn- remove-from-world [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? ids->eids id))
    (alter-var-root #'ids->eids dissoc id))
  (content-grid-remove-entity! eid)
  (grid-remove-entity eid))

(defn position-changed [eid]
  (content-grid-update-entity! content-grid eid)
  (grid-entity-position-changed eid))

(defn all-entities []
  (vals ids->eids))

(defn remove-destroyed []
  (doseq [eid (filter (comp :entity/destroyed? deref) (all-entities))]
    (remove-from-world eid)
    (doseq [component @eid]
      (e-destroy component eid))))

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

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-body-size 0.39) ; == spider smallest creature size.

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-body-size max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form
(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(def ^:private z-orders [:z-order/on-ground
                         :z-order/ground
                         :z-order/flying
                         :z-order/effect])

(def render-z-order (define-order z-orders))

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
  (assert (>= width  (if collides? minimum-body-size 0)))
  (assert (>= height (if collides? minimum-body-size 0)))
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

(defn- spawn-entity [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      (create-vs)))))]
    (add-to-world eid)
    (doseq [component @eid]
      (e-create component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [position {:keys [tx/sound entity/animation]}]
  (play-sound sound)
  (spawn-entity position
                effect-body-props
                {:entity/animation animation
                 :entity/delete-after-animation-stopped true}))

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
  (let [props (db/build creature-id)]
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
                 {:counter (timer duration)
                  :faction faction}}))

(defn spawn-line-render [{:keys [start end duration color thick?]}]
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
                   :rotation-angle (v-angle-from-vector direction)}
                  {:entity/movement {:direction direction
                                     :speed speed}
                   :entity/image image
                   :entity/faction faction
                   :entity/delete-after-duration (/ max-range speed)
                   :entity/destroy-audiovisual :audiovisuals/hit-wall
                   :entity/projectile-collision {:entity-effects entity-effects
                                                 :piercing? piercing?}})))

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- spawn-player [start-position]
  (spawn-creature {:position (tile->middle start-position)
                   :creature-id :creatures/vampire
                   :components {:entity/fsm {:fsm :fsms/player
                                             :initial-state :player-idle}
                                :entity/faction :good
                                :entity/player? true
                                :entity/free-skill-points 3
                                :entity/clickable {:type :clickable/player}
                                :entity/click-distance-tiles 1.5}}))

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature (update props :position tile->middle))))

(defn world-init [{:keys [tiled-map start-position]}]
  (bind-root #'world-tiled-map tiled-map)
  (bind-root #'explored-tile-corners (atom (grid2d (tm-width  tiled-map)
                                                   (tm-height tiled-map)
                                                   (constantly false))))
  (bind-root #'world-grid (grid2d (tm-width tiled-map)
                                  (tm-height tiled-map)
                                  (fn [position]
                                    (atom (->cell position
                                                  (case (movement-property tiled-map position)
                                                    "none" :none
                                                    "air"  :air
                                                    "all"  :all))))))

  (init-raycaster world-grid blocks-vision?)
  (let [width  (tm-width  tiled-map)
        height (tm-height tiled-map)]
    (bind-root #'content-grid (content-grid-create {:cell-size 16  ; FIXME global config
                                                    :width  width
                                                    :height height})))
  (bind-root #'tick-error nil)
  (bind-root #'ids->eids {})
  (bind-root #'elapsed-time 0)
  (bind-root #'world-delta nil)
  (bind-root #'player-eid (spawn-player start-position))
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn active-entities []
  (content-grid-active-entities content-grid @player-eid))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam-position (world-camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float world-viewport-width)  2)))
     (<= ydist (inc (/ (float world-viewport-height) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(defn render-entities
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [render-below
                    render-default
                    render-above
                    render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

(defn e-direction [entity other-entity]
  (v-direction (:position entity) (:position other-entity)))

(defn e-collides? [entity other-entity]
  (overlaps? entity other-entity))

(defn e-tile [entity]
  (->tile (:position entity)))

(defn e-enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn e-state-k [entity]
  (-> entity :entity/fsm :state))

(defn e-state-obj [entity]
  (let [k (e-state-k entity)]
    [k (k entity)]))

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (e-state-obj @eid)
              new-state-obj [new-state-k (->v (if params
                                                [new-state-k eid params]
                                                [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [cursor (state-cursor new-state-obj)]
              (set-cursor cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (state-exit old-state-obj)
          (state-enter new-state-obj))))))

(defn send-event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))


(defn add-mods    [entity mods] (update entity :entity/modifiers mods-add    mods))
(defn remove-mods [entity mods] (update entity :entity/modifiers mods-remove mods))

(defn mod-value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops-apply (modifier-k modifiers)
             base-value))

(defn e-stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               entity
               (keyword "modifier" (name k)))))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn- apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max-schema val-max)]
   :post [(m/validate val-max-schema val-max)]}
  (let [val-max (update val-max 1 mod-value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn- apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max-schema val-max)]
   :post [(m/validate val-max-schema val-max)]}
  (let [val-max (update val-max 0 mod-value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (apply-max-modifier entity :modifier/hp-max)))

(defn e-mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (apply-max-modifier entity :modifier/mana-max)))

(defn pay-mana-cost [entity cost]
  (let [mana-val ((e-mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defn add-text-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))

(defn- mana-value [entity]
  (if (:entity/mana entity)
    ((e-mana entity) 0)
    0))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn skill-usable-state
  [entity
   {:keys [skill/cooling-down? skill/effects] :as skill}
   effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effects-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn damage-mods
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier source :modifier/damage-deal-min)
                (apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage-mods source damage)
           :damage/min-max
           apply-max-modifier
           target
           :modifier/damage-receive-max)))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (.setUserObject actor (button-group {:max-check-count 1 :min-check-count 0}))
    actor))

(defn- group->button-group [group]
  (.getUserObject (Group/.findActor group "action-bar/button-group")))

(defn- get-action-bar []
  (let [group (::action-bar (:action-bar-table (screen-stage)))]
    {:horizontal-group group
     :button-group (group->button-group group)}))

(defn actionbar-add-skill [{:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        button (image-button image (fn []) {:scale 2})]
    (.setUserObject button id)
    (add-tooltip! button #(info-text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn actionbar-remove-skill [{:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))

(defn actionbar-create []
  (let [group (horizontal-group {:pad 2 :space 2})]
    (.setUserObject group ::action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn actionbar-selected-skill []
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar)))]
    (.getUserObject skill-button)))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (has-skill? entity skill))]}
  (when (:entity/player? entity)
    (actionbar-add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove-skill [entity {:keys [property/id] :as skill}]
  {:pre [(has-skill? entity skill)]}
  (when (:entity/player? entity)
    (actionbar-remove-skill skill))
  (update entity :entity/skills dissoc id))
