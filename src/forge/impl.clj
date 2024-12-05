(ns forge.impl
  (:require [clj-commons.pretty.repl :as pretty-repl]
            [clojure.component :refer [defmethods]]
            [clojure.math :as math]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [data.grid2d :as g2d]
            [forge.core :refer :all]
            [forge.lifecycle :as lifecycle]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Colors  Texture Texture$TextureFilter Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.maps.tiled TmxMapLoader TiledMapTileLayer)
           (com.badlogic.gdx.scenes.scene2d Actor Stage)
           (com.badlogic.gdx.utils Align Scaling ScreenUtils )
           (com.badlogic.gdx.math MathUtils Vector2 Circle Intersector Rectangle)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (space.earlygrey.shapedrawer ShapeDrawer)
           (forge OrthogonalTiledMapRenderer)))

(defn-impl pretty-pst [t]
  (binding [*print-level* 3]
    (pretty-repl/pretty-pst t 24)))

(def-impl str-join          str/join)
(def-impl str-upper-case    str/upper-case)
(def-impl str-replace       str/replace)
(def-impl str-replace-first str/replace-first)
(def-impl str-split         str/split)
(def-impl str-capitalize    str/capitalize)
(def-impl str-trim-newline  str/trim-newline)
(def-impl signum            math/signum)
(def-impl set-difference    set/difference)
(def-impl pprint            pprint/pprint)

(extend-type Actor
  HasUserObject
  (user-object [actor]
    (.getUserObject actor)))

(extend-type com.badlogic.gdx.scenes.scene2d.Group
  Group
  (children [group]
    (seq (.getChildren group)))

  (clear-children [group]
    (.clearChildren group))

  (add-actor! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name)))

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

(defn- load-assets [folder]
  (let [manager (asset-manager)]
    (doseq [[class exts] [[com.badlogic.gdx.audio.Sound #{"wav"}]
                          [Texture #{"png" "bmp"}]]
            file (map #(str-replace-first % folder "")
                      (recursively-search (internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defmethods :app/assets
  (lifecycle/create [[_ folder]]
    (bind-root #'assets (load-assets folder)))
  (lifecycle/dispose [_]
    (dispose assets)))

(def-impl black Color/BLACK)
(def-impl white Color/WHITE)

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn-impl ttfont [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (.internal Gdx/files file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str-replace (str-upper-case (name k)) "-" "_")))))

(def ^:private k->input-button (partial static-field "Input$Buttons"))
(def ^:private k->input-key    (partial static-field "Input$Keys"))

(defn-impl equal? [a b]
  (MathUtils/isEqual a b))

(defn-impl clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn-impl degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn-impl frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn-impl delta-time []
  (.getDeltaTime Gdx/graphics))

(defn-impl button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn-impl key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn-impl key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(defn-impl set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn-impl internal-file [path]
  (.internal Gdx/files path))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str-split #"\n")
      count
      (* (.getLineHeight font))))

(defn- gdx-align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn- gdx-scaling [k]
  (case k
    :fill Scaling/fill))

(defn-impl draw-text
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

(defn-impl ->color
  ([r g b]
   (->color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a)))
  (^Color [c]
          (cond (= Color (class c)) c
                (keyword? c) (static-field "graphics.Color" c)
                (vector? c) (apply ->color c)
                :else (throw (ex-info "Cannot understand color" c)))))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose [obj]
    (.dispose obj)))

(extend-type com.badlogic.gdx.audio.Sound
  Sound
  (play [sound]
    (.play sound)))

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
  (lifecycle/create [[_ skin-scale]]
    (check-cleanup-visui!)
    (VisUI/load (case skin-scale
                  :skin-scale/x1 VisUI$SkinScale/X1
                  :skin-scale/x2 VisUI$SkinScale/X2))
    (font-enable-markup!)
    (set-tooltip-config!))
  (lifecycle/dispose [_]
    (VisUI/dispose)))

(defn-impl sprite-batch []
  (SpriteBatch.))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(let [pixel-texture (atom nil)]
  (defmethods :app/shape-drawer
    (lifecycle/create [_]
      (reset! pixel-texture (white-pixel-texture))
      (bind-root #'shape-drawer (ShapeDrawer. batch (TextureRegion. ^Texture @pixel-texture 1 0 1 1))))
    (lifecycle/dispose [_]
      (dispose @pixel-texture))))

(defmethods :app/cursors
  (lifecycle/create [[_ data]]
    (bind-root #'cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                    (let [pixmap (Pixmap. (internal-file (str "cursors/" file ".png")))
                                          cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                      (dispose pixmap)
                                      cursor))
                                  data)))
  (lifecycle/dispose [_]
    (run! dispose (vals cursors))))

(defmethods :app/gui-viewport
  (lifecycle/create [[_ [width height]]]
    (bind-root #'gui-viewport-width  width)
    (bind-root #'gui-viewport-height height)
    (bind-root #'gui-viewport (FitViewport. width height (OrthographicCamera.))))
  (lifecycle/resize [_ w h]
    (.update gui-viewport w h true)))

(defmethods :app/world-viewport
  (lifecycle/create [[_ [width height tile-size]]]
    (bind-root #'world-unit-scale (float (/ tile-size)))
    (bind-root #'world-viewport-width  width)
    (bind-root #'world-viewport-height height)
    (bind-root #'world-viewport (let [world-width  (* width  world-unit-scale)
                                      world-height (* height world-unit-scale)
                                      camera (OrthographicCamera.)
                                      y-down? false]
                                  (.setToOrtho camera y-down? world-width world-height)
                                  (FitViewport. world-width world-height camera))))
  (lifecycle/resize [_ w h]
    (.update world-viewport w h true)))

(defmethods :app/cached-map-renderer
  (lifecycle/create [_]
    (bind-root #'cached-map-renderer
      (memoize
       (fn [tiled-map]
         (OrthogonalTiledMapRenderer. tiled-map
                                      (float world-unit-scale)
                                      batch))))))

(extend-type Stage
  Acting
  (act [this]
    (.act this))
  Drawing
  (draw [this]
    (.draw this)))

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
  (lifecycle/create [[_ {:keys [ks first-k]}]]
    (bind-root #'screens (mapvals stage-screen (mapvals
                                                (fn [ns-sym]
                                                  (require ns-sym)
                                                  ((ns-resolve ns-sym 'create)))
                                                ks)))
    (change-screen first-k))
  (lifecycle/dispose [_]
    (run! screen-destroy (vals screens)))
  (lifecycle/render [_]
    (ScreenUtils/clear black)
    (screen-render (current-screen))))

(defn-impl add-actor [actor]
  (.addActor (screen-stage) actor))

(defn-impl reset-stage [new-actors]
  (.clear (screen-stage))
  (run! add-actor new-actors))

(def-impl grid2d                    g2d/create-grid)
(def-impl g2d-width                 g2d/width)
(def-impl g2d-height                g2d/height)
(def-impl g2d-cells                 g2d/cells)
(def-impl g2d-posis                 g2d/posis)
(def-impl get-4-neighbour-positions g2d/get-4-neighbour-positions)
(def-impl mapgrid->vectorgrid       g2d/mapgrid->vectorgrid)

(defn- m-v2
  (^Vector2 [[x y]] (Vector2. x y))
  (^Vector2 [x y]   (Vector2. x y)))

(defn- ->p [^Vector2 v]
  [(.x v) (.y v)])

(defn-impl v-scale [v n]
  (->p (.scl (m-v2 v) (float n))))

(defn-impl v-normalise [v]
  (->p (.nor (m-v2 v))))

(defn-impl v-add [v1 v2]
  (->p (.add (m-v2 v1) (m-v2 v2))))

(defn-impl v-length [v]
  (.len (m-v2 v)))

(defn-impl v-distance [v1 v2]
  (.dst (m-v2 v1) (m-v2 v2)))

(defn-impl v-normalised? [v]
  (equal? 1 (v-length v)))

(defn-impl v-direction [[sx sy] [tx ty]]
  (v-normalise [(- (float tx) (float sx))
                (- (float ty) (float sy))]))

(defn-impl v-angle-from-vector [v]
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

(defn-impl overlaps? [a b]
  (overlaps?* (m->shape a) (m->shape b)))

(defn-impl rect-contains? [rectangle [x y]]
  (Rectangle/.contains (m->shape rectangle) x y))

#_(defprotocol Schema
    (s-explain  [_ value])
    (s-form     [_])
    (s-validate [_ data]))

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(extend-type clojure.lang.APersistentMap
  Property
  (validate! [property]
    (let [m-schema (-> property
                       schema-of-property
                       malli-form
                       m/schema)]
      (when-not (m/validate m-schema property)
        (throw (invalid-ex-info m-schema property))))))

(def-impl val-max-schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(defmethod malli-form :s/val-max [_]
  (m/form val-max-schema))

(defn-impl val-max-ratio
  [[^int v ^int mx]]
  {:pre [(m/validate val-max-schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))

(defn-impl load-tmx-map
  [file]
  (.load (TmxMapLoader.) file))

(defn-impl add-color [name-str color]
  (Colors/put name-str (->color color)))

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

(extend-type com.badlogic.gdx.graphics.g2d.Batch
  Batch
  (draw-texture-region [this texture-region [x y] [w h] rotation color]
    (if color (.setColor this color))
    (.draw this
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
    (if color (.setColor this white)))

  (draw-on-viewport [this viewport draw-fn]
    (.setColor this white) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix this (.combined (Viewport/.getCamera viewport)))
    (.begin this)
    (draw-fn)
    (.end this)))

(defn- sd-color [color]
  (.setColor shape-drawer (->color color)))

(defn-impl draw-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn-impl draw-filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn-impl draw-circle [[x y] radius color]
  (sd-color color)
  (.circle shape-drawer (float x) (float y) (float radius)))

(defn-impl draw-filled-circle [[x y] radius color]
  (sd-color color)
  (.filledCircle shape-drawer (float x) (float y) (float radius)))

(defn-impl draw-arc [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.arc shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn-impl draw-sector [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.sector shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn-impl draw-rectangle [x y w h color]
  (sd-color color)
  (.rectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn-impl draw-filled-rectangle [x y w h color]
  (sd-color color)
  (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn-impl draw-line [[sx sy] [ex ey] color]
  (sd-color color)
  (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))

(defn-impl draw-grid [leftx bottomy gridw gridh cellw cellh color]
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

(defn-impl with-line-width [width draw-fn]
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

(defn-impl gui-mouse-position []
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position gui-viewport)))

(defn-impl world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn-impl world-camera []
  (.getCamera world-viewport))

(defn-impl ->texture-region
  ([path]
   (TextureRegion. ^Texture (get assets path)))
  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))

(defn-impl ->image [path]
  (sprite* world-unit-scale
           (->texture-region path)))

(defn-impl sub-image [image bounds]
  (sprite* world-unit-scale
           (->texture-region (:texture-region image) bounds)))

(defn-impl sprite-sheet [path tilew tileh]
  {:image (->image path)
   :tilew tilew
   :tileh tileh})

(defn-impl ->sprite [{:keys [image tilew tileh]} [x y]]
  (sub-image image
             [(* x tilew) (* y tileh) tilew tileh]))

(defn-impl draw-image [{:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image *unit-scale*)
                       0 ; rotation
                       color))

(defn-impl draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image *unit-scale*)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn-impl draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- draw-with [viewport unit-scale draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(with-line-width unit-scale
                       (fn []
                         (binding [*unit-scale* unit-scale]
                           (draw-fn))))))

(defn-impl draw-on-world-view [render-fn]
  (draw-with world-viewport
             world-unit-scale
             render-fn))

(defmethods :app/db
  (lifecycle/create [[_ config]]
    (db-init config)))

(defmethods :app/default-font
  (lifecycle/create [[_ font]]
    (bind-root #'default-font (ttfont font)))
  (lifecycle/dispose [_]
    (dispose default-font)))

(defmethods :app/sprite-batch
  (lifecycle/create [_]
    (bind-root #'batch (sprite-batch)))
  (lifecycle/dispose [_]
    (dispose batch)))
