(ns forge.lifecycle
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Texture Texture$TextureFilter Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils ScreenUtils SharedLibraryLoader)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (org.lwjgl.system Configuration)
           (java.awt Taskbar Toolkit)
           (space.earlygrey.shapedrawer ShapeDrawer)
           (forge OrthogonalTiledMapRenderer)))

(defn- set-dock-icon [resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource resource))))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(def ^:private mac? SharedLibraryLoader/isMac)

(defn- configure-lwjgl [{:keys [glfw-library-name glfw-check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME  glfw-library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 glfw-check-thread0))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- asset-descriptons [folder]
  (for [[class exts] [[Sound   #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(defn- asset-manager* ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- load-assets [assets]
  (let [manager (asset-manager*)]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

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
    (dispose stage)
    (screen-destroy sub-screen)))

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

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- ttfont [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (.internal Gdx/files file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defsystem create)
(defmethod create :default [_])

(defsystem destroy)
(defmethod destroy :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods :app/assets
  (create [[_ folder]]
    (bind-root #'asset-manager (load-assets (asset-descriptons folder))))
  (destroy [_]
    (dispose asset-manager)))

(defmethods :app/vis-ui
  (create [[_ skin-scale]]
    ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
    ; => actually there is a deeper issue at play
    ; we need to dispose ALL resources which were loaded already ...
    (when (VisUI/isLoaded)
      (VisUI/dispose))
    (VisUI/load (case skin-scale
                  :skin-scale/x1 VisUI$SkinScale/X1
                  :skin-scale/x2 VisUI$SkinScale/X2))
    (-> (VisUI/getSkin)
        (.getFont "default-font")
        .getData
        .markupEnabled
        (set! true))
    ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
    ;Controls whether to fade out tooltip when mouse was moved. (default false)
    ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
    (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))
  (destroy [_]
    (VisUI/dispose)))

(defmethods :app/sprite-batch
  (create [_]
    (bind-root #'batch (SpriteBatch.)))
  (destroy [_]
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
    (create [_]
      (reset! pixel-texture (white-pixel-texture))
      (bind-root #'shape-drawer (ShapeDrawer. batch (TextureRegion. ^Texture @pixel-texture 1 0 1 1))))
    (destroy [_]
      (dispose @pixel-texture))))

(let [cursors (atom nil)]
  (defmethods :app/cursors
    (create [[_ data]]
      (reset! cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                 (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                       cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                   (dispose pixmap)
                                   cursor))
                               data)))
    (destroy [_]
      (run! dispose (vals @cursors))))

  (defn-impl set-cursor [cursor-key]
    (.setCursor Gdx/graphics (safe-get @cursors cursor-key))))

(defmethods :app/gui-viewport
  (create [[_ [width height]]]
    (bind-root #'gui-viewport-width  width)
    (bind-root #'gui-viewport-height height)
    (bind-root #'gui-viewport (FitViewport. width height (OrthographicCamera.))))
  (resize [_ w h]
    (.update gui-viewport w h true)))

(defmethods :app/world-viewport
  (create [[_ [width height tile-size]]]
    (bind-root #'world-unit-scale (float (/ tile-size)))
    (bind-root #'world-viewport-width  width)
    (bind-root #'world-viewport-height height)
    (bind-root #'world-viewport (let [world-width  (* width  world-unit-scale)
                                      world-height (* height world-unit-scale)
                                      camera (OrthographicCamera.)
                                      y-down? false]
                                  (.setToOrtho camera y-down? world-width world-height)
                                  (FitViewport. world-width world-height camera))))
  (resize [_ w h]
    (.update world-viewport w h true)))

(defmethods :app/cached-map-renderer
  (create [_]
    (bind-root #'cached-map-renderer
               (memoize
                (fn [tiled-map]
                  (OrthogonalTiledMapRenderer. tiled-map
                                               (float world-unit-scale)
                                               batch))))))

(defmethods :app/screens
  (create [[_ {:keys [ks first-k]}]]
    (bind-root #'screens (mapvals stage-screen (mapvals
                                                (fn [ns-sym]
                                                  (require ns-sym)
                                                  ((ns-resolve ns-sym 'create)))
                                                ks)))
    (change-screen first-k))
  (destroy [_]
    (run! screen-destroy (vals screens)))
  (render [_]
    (ScreenUtils/clear Color/BLACK)
    (screen-render (current-screen))))

(defmethods :app/db
  (create [[_ {:keys [schema properties]}]]
    (bind-root #'schemas (-> schema io/resource slurp edn/read-string))
    (bind-root #'properties-file (io/resource properties))
    (let [properties (-> properties-file slurp edn/read-string)]
      (assert (or (empty? properties)
                  (apply distinct? (map :property/id properties))))
      (run! validate! properties)
      (bind-root #'db-properties (zipmap (map :property/id properties) properties)))))

(defmethods :app/default-font
  (create [[_ font]]
    (bind-root #'default-font (ttfont font)))
  (destroy [_]
    (dispose default-font)))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    (set-dock-icon (:dock-icon config))
    (when mac?
      (configure-lwjgl {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create  []    (run! create          components))
                          (dispose []    (run! destroy         components))
                          (render  []    (run! render          components))
                          (resize  [w h] (run! #(resize % w h) components)))
                        (lwjgl3-config (:lwjgl3 config)))))
