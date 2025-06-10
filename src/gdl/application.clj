(ns gdl.application
  (:require [clojure.space.earlygrey.shape-drawer :as sd]
            [clojure.edn :as edn]
            [clojure.gdx.interop :as interop]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [gdl.audio]
            [gdl.graphics]
            [gdl.graphics.camera]
            [gdl.graphics.color :as color]
            [gdl.graphics.texture-region :as texture-region]
            [gdl.graphics.viewport]
            [gdl.input]
            [gdl.math.utils :as math-utils]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector2
                                  Vector3)
           (com.badlogic.gdx.utils Disposable
                                   SharedLibraryLoader
                                   ScreenUtils
                                   Os)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defn- require-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (if (namespace form)
                       (requiring-resolve form)
                       (do (require form) form))
                     form))
                 form))

(defn- load-edn-config [path]
  (let [m (-> path
              io/resource
              slurp
              edn/read-string
              require-symbols)]
    (reify clojure.lang.ILookup
      (valAt [_ k]
        (assert (contains? m k)
                (str "Config key not found: " k))
        (get m k)))))

(defn- reify-stage [stage]
  (reify
    ; TODO is disposable but not sure if needed as we handle batch ourself.
    clojure.lang.ILookup
    (valAt [_ key]
      (key stage))

    stage/Stage
    (render! [_ ctx]
      (ui/act! stage ctx)
      (ui/draw! stage ctx)
      ctx)

    (add! [_ actor] ; -> re-use gdl.ui/add! ?
      (ui/add! stage actor))

    (clear! [_]
      (ui/clear! stage))

    (hit [_ position]
      (ui/hit stage position))

    (find-actor [_ actor-name]
      (-> stage
          ui/root
          (ui/find-actor actor-name)))))

(defn- create-stage! [ui-config graphics]
  (ui/load! ui-config)
  (let [stage (ui/stage (:java-object (:ui-viewport graphics))
                        (:batch graphics))]
    (.setInputProcessor Gdx/input stage)
    (reify-stage stage)))

(defn- create-input []
  (let [this Gdx/input]
    (reify gdl.input/Input
      (button-just-pressed? [_ button]
        (.isButtonJustPressed this (interop/k->input-button button)))

      (key-pressed? [_ key]
        (.isKeyPressed this (interop/k->input-key key)))

      (key-just-pressed? [_ key]
        (.isKeyJustPressed this (interop/k->input-key key)))

      (mouse-position [_]
        [(.getX this)
         (.getY this)]))))

(defn- freetype-font-params [{:keys [size
                                     min-filter
                                     mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn- generate-font
  [file-handle
   {:keys [size
           quality-scaling
           enable-markup?
           use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        ^BitmapFont font (.generateFont generator
                                        (freetype-font-params {:size (* size quality-scaling)
                                                               ; :texture-filter/linear because scaling to world-units
                                                               :min-filter Texture$TextureFilter/Linear
                                                               :mag-filter Texture$TextureFilter/Linear}))]
    (.setScale (.getData font) (/ quality-scaling))
    (set! (.markupEnabled (.getData font)) enable-markup?)
    (.setUseIntegerPositions font use-integer-positions?)
    font))

(defn- draw-texture-region! [^SpriteBatch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn- texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   texture-region]
  (let [dimensions (texture-region/dimensions texture-region)]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defmulti draw! (fn [[k] _graphics]
                  k))

(defmethod draw! :draw/texture-region [[_ texture-region [x y]]
                                       {:keys [batch]}]
  (draw-texture-region! batch
                        texture-region
                        [x y]
                        (texture-region/dimensions texture-region)
                        0  ;rotation
                        ))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)]
    (draw-texture-region! batch
                          texture-region
                          position
                          (texture-region-drawing-dimensions graphics texture-region)
                          0 ; rotation
                          )))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (draw-texture-region! batch
                          texture-region
                          [(- (float x) (/ (float w) 2))
                           (- (float y) (/ (float h) 2))]
                          [w h]
                          rotation
                          )))

(defmethod draw! :draw/centered [[_ image position] this]
  (draw! [:draw/rotated-centered image 0 position] this))

(defn- bitmap-font-text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- draw-bitmap-font! [{:keys [^BitmapFont font batch text x y target-width align wrap?]}]
  (.draw font
         batch
         text
         (float x)
         (float y)
         (float target-width)
         align
         wrap?))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [batch
                                     unit-scale
                                     default-font]}]
  (let [^BitmapFont font (or font default-font)
        scale (* (float @unit-scale)
                 (float (or scale 1)))
        old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (float (* old-scale scale)))
    (draw-bitmap-font! {:font font
                        :batch batch
                        :text text
                        :x x
                        :y (+ y (if up? (bitmap-font-text-height font text) 0))
                        :target-width 0
                        :align (interop/k->align (or h-align :center))
                        :wrap? false})
    (.setScale (.getData font) (float old-scale))))

(defn- sd-set-color! [shape-drawer color]
  (sd/set-color! shape-drawer (color/create color)))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (math-utils/degree->radians start-angle)
           (math-utils/degree->radians degree)))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (math-utils/degree->radians start-angle)
              (math-utils/degree->radians degree)))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] this]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] this))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] this))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (gdl.graphics/handle-draws! this draws))))

(defrecord Graphics [graphics
                     textures
                     cursors
                     default-font
                     world-unit-scale
                     ui-viewport
                     world-viewport
                     ^SpriteBatch batch
                     unit-scale
                     shape-drawer-texture
                     shape-drawer
                     tiled-map-renderer]
  gdl.utils.disposable/Disposable
  (dispose! [_]
    (Disposable/.dispose batch)
    (gdl.utils.disposable/dispose! shape-drawer-texture)
    (run! gdl.utils.disposable/dispose! (vals textures))
    (run! gdl.utils.disposable/dispose! (vals cursors))
    (when default-font
      (gdl.utils.disposable/dispose! default-font)))

  gdl.graphics/Graphics
  (clear-screen! [_ color]
    (ScreenUtils/clear (color/create color)))

  (resize-viewports! [_ width height]
    (gdl.graphics.viewport/resize! ui-viewport    width height)
    (gdl.graphics.viewport/resize! world-viewport width height))

  (delta-time [_]
    (.getDeltaTime graphics))

  (frames-per-second [_]
    (.getFramesPerSecond graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (.setCursor graphics (get cursors cursor-key)))

  ; TODO probably not needed I only work with texture-regions
  (texture [_ path]
    (assert (contains? textures path)
            (str "Cannot find texture with path: " (pr-str path)))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    (.setColor batch (color/create :white)) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (gdl.graphics.camera/combined (:camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:camera world-viewport)]
      (.setColorSetter renderer (reify ColorSetter
                                  (apply [_ color x y]
                                    (color-setter color x y))))
      (.setView renderer (:camera/java-object camera))
      ; there is also:
      ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
      ; but right order / visible only ?
      (->> tiled-map
           tiled/layers
           (filter tiled/visible?)
           (map (partial tiled/layer-index tiled-map))
           int-array
           (.render renderer))))

  ; FIXME this can be memoized
  ; also good for tiled-map tiles they have to be memoized too
  (image->texture-region [graphics {:keys [image/file
                                           image/bounds]}]
    (assert file)
    (let [texture (gdl.graphics/texture graphics file)]
      (if bounds
        (apply texture-region/create texture bounds)
        (texture-region/create texture))))

  (handle-draws! [this draws]
    (doseq [component draws
            :when component]
      (draw! component this))))

(defn- reify-camera [^OrthographicCamera this]
  (reify
    clojure.lang.ILookup
    (valAt [_ k]
      (case k
        :camera/java-object this))

    gdl.graphics.camera/Camera
    (zoom [_]
      (.zoom this))

    (position [_]
      [(.x (.position this))
       (.y (.position this))])

    (combined [_]
      (.combined this))

    (frustum [_]
      (let [frustum-points (take 4 (map (fn [^Vector3 v3]
                                          [(.x v3)
                                           (.y v3)
                                           (.z v3)])
                                        (.planePoints (.frustum this))))
            left-x   (apply min (map first  frustum-points))
            right-x  (apply max (map first  frustum-points))
            bottom-y (apply min (map second frustum-points))
            top-y    (apply max (map second frustum-points))]
        [left-x right-x bottom-y top-y]))

    (set-position! [_ [x y]]
      (set! (.x (.position this)) (float x))
      (set! (.y (.position this)) (float y))
      (.update this))

    (set-zoom! [_ amount]
      (set! (.zoom this) amount)
      (.update this))

    (viewport-width [_]
      (.viewportWidth this))

    (viewport-height [_]
      (.viewportHeight this))

    (reset-zoom! [cam]
      (gdl.graphics.camera/set-zoom! cam 1))

    (inc-zoom! [cam by]
      (gdl.graphics.camera/set-zoom! cam (max 0.1 (+ (.zoom this) by)))) ))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      gdl.graphics.viewport/Viewport
      (resize! [_ width height]
        (.update this width height (boolean center-camera?)))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (unproject [_ [x y]]
        (let [x (math-utils/clamp x (.getLeftGutterWidth this) (.getRightGutterX    this))
              y (math-utils/clamp y (.getTopGutterHeight this) (.getTopGutterY      this))]
          (let [vector2 (.unproject this (Vector2. x y))]
            [(.x vector2)
             (.y vector2)])))

      clojure.lang.ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (reify-camera (.getCamera this)))))))

(defn- create-ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (OrthographicCamera.)
                {:center-camera? true}))

(defn- create-world-viewport [world-unit-scale {:keys [width height]}]
  (let [world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)]
    (fit-viewport world-width
                  world-height
                  (doto (OrthographicCamera.)
                    (.setToOrtho false ; y-down ?
                                 world-width
                                 world-height))
                  {:center-camera? false})))

(defn- white-pixel-texture [graphics]
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-graphics [gdx-graphics
                        {:keys [textures
                                colors ; optional
                                cursors ; optional
                                cursor-path-format ; optional
                                default-font ; optional, could use gdx included (BitmapFont.)
                                tile-size
                                ui-viewport
                                world-viewport]}]
  (doseq [[name color-params] colors]
    (Colors/put name (color/create color-params)))
  ;(println "load-textures (count textures): " (count textures))
  (let [batch (SpriteBatch.)
        shape-drawer-texture (white-pixel-texture gdx-graphics)
        world-unit-scale (float (/ tile-size))
        ui-viewport (create-ui-viewport ui-viewport)
        textures (into {} (for [file textures]
                            [file (Texture. file)]))
        cursors (update-vals cursors
                             (fn [[file [hotspot-x hotspot-y]]]
                               (let [pixmap (Pixmap. (.internal Gdx/files (format cursor-path-format file)))
                                     cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                 (.dispose pixmap)
                                 cursor)))]
    (map->Graphics {:graphics gdx-graphics
                    :textures textures
                    :cursors cursors
                    :default-font (when default-font
                                    (generate-font (.internal Gdx/files (:file default-font))
                                                   (:params default-font)))
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (create-world-viewport world-unit-scale world-viewport)
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (sd/create batch
                                             (texture-region/create shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))

(defn- create-audio [sounds-to-load]
  ;(println "create-audio. (count sounds-to-load): " (count sounds-to-load))
  (let [sounds (into {}
                     (for [file sounds-to-load]
                       [file (.newSound Gdx/audio (.internal Gdx/files file))]))]
    (reify
      gdl.utils.disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! Disposable/.dispose (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (Sound/.play (get sounds path))))))

(defn- recursively-search [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(extend-type Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- find-assets [{:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (recursively-search (.internal Gdx/files folder)
                           extensions)))

(defn- create-context [main-config]
  (let [config (::context main-config)
        graphics-config (update (:graphics config) :textures find-assets)
        graphics (create-graphics Gdx/graphics graphics-config)
        stage (create-stage! (:ui config) graphics)]
    {:ctx/config main-config
     :ctx/input (create-input)
     :ctx/audio (when (:sounds config)
                  (create-audio (find-assets (:sounds config))))
     :ctx/graphics graphics
     :ctx/stage stage}))

(defn- set-mac-settings! [{:keys [glfw-async? dock-icon]}]
  (when glfw-async?
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (when dock-icon
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))))

(defn- create-lwjgl-app-config [lwjgl-app-config]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle (:title lwjgl-app-config))
    (.setWindowedMode (:width  (:windowed-mode lwjgl-app-config))
                      (:height (:windowed-mode lwjgl-app-config)))
    (.setForegroundFPS (:foreground-fps lwjgl-app-config))))

(defn- create-app-listener [{:keys [create dispose render resize pause resume]} config]
  (proxy [ApplicationListener] []
    (create  []              (when-let [[f params] create] (f (create-context config) params)))
    (dispose []              (when dispose (dispose)))
    (render  []              (when-let [[f params] render] (f params)))
    (resize  [width height]  (when resize  (resize width height)))
    (pause   []              (when pause   (pause)))
    (resume  []              (when resume  (resume)))))

(defn- mac-os? []
  (= SharedLibraryLoader/os Os/MacOsX))

(defn -main [config-path]
  (let [config (load-edn-config config-path)]
    (when (mac-os?)
      (set-mac-settings! (::mac-os-settings config)))
    (Lwjgl3Application. (create-app-listener (::listener config)
                                             config)
                        (create-lwjgl-app-config (::lwjgl-app-config config)))))

(defn post-runnable! [runnable]
  (.postRunnable Gdx/app runnable))
