(ns forge.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture Texture$TextureFilter Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils SharedLibraryLoader ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (forge OrthogonalTiledMapRenderer)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defsystem create)

(defsystem destroy)
(defmethod destroy :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

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

(defn- load-assets [folder]
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [^String path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[class exts] [[Sound   #{"wav"}]
                          [Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (recursively-search (.internal Gdx/files folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defmethods :lifecycle/asset-manager
  (create [[_ folder]]
    (bind-root #'asset-manager (load-assets folder)))
  (destroy [_]
    (dispose asset-manager)))

(defmethods :lifecycle/sprite-batch
  (create [_]
    (bind-root #'batch (SpriteBatch.)))
  (destroy [_]
    (dispose batch)))

(declare ^:private ^Texture shape-drawer-texture)

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(defmethods :lifecycle/shape-drawer
  (create [_]
    (bind-root #'shape-drawer-texture (white-pixel-texture))
    (bind-root #'shape-drawer (ShapeDrawer. batch (TextureRegion. shape-drawer-texture 1 0 1 1))))
  (destroy [_]
    (dispose shape-drawer-texture)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- truetype-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (.internal Gdx/files file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defmethods :lifecycle/default-font
  (create [[_ font]]
    (bind-root #'default-font (truetype-font font)))
  (destroy [_]
    (dispose default-font)))

(defmethods :lifecycle/cursors
  (create [[_ cursors]]
    (bind-root #'k->cursor (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                      (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                            cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                        (dispose pixmap)
                                        cursor))
                                    cursors)))
  (destroy [_]
    (run! dispose (vals k->cursor))))

(defmethods :lifecycle/cached-map-renderer
  (create [_]
    (bind-root #'cached-map-renderer (memoize
                                      (fn [tiled-map]
                                        (OrthogonalTiledMapRenderer. tiled-map
                                                                     (float world-unit-scale)
                                                                     batch))))))
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

(defmethods :lifecycle/vis-ui
  (create [[_ skin-scale]]
    (check-cleanup-visui!)
    (VisUI/load (case skin-scale
                  :skin-scale/x1 VisUI$SkinScale/X1
                  :skin-scale/x2 VisUI$SkinScale/X2))
    (font-enable-markup!)
    (set-tooltip-config!))
  (destroy [_]
    (VisUI/dispose)))

(defmethods :lifecycle/gui-viewport
  (create [[_ [width height]]]
    (bind-root #'gui-viewport-width  width)
    (bind-root #'gui-viewport-height height)
    (bind-root #'gui-viewport (FitViewport. width height (OrthographicCamera.))))
  (resize [_ w h]
    (.update gui-viewport w h true)))

(defmethods :lifecycle/world-viewport
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

(defmethods :lifecycle/screens
  (create [[_ {:keys [ks first-k]}]]
    (bind-root #'app-screens (mapvals stage-screen (mapvals
                                                    (fn [ns-sym]
                                                      (require ns-sym)
                                                      ((ns-resolve ns-sym 'create)))
                                                    ks)))
    (change-screen first-k))
  (destroy [_]
    (run! screen-destroy (vals app-screens)))
  (render [_]
    (ScreenUtils/clear black)
    (screen-render (current-screen))))

(defmethods :lifecycle/db
  (create [[_ {:keys [schema properties]}]]
    (bind-root #'db-schemas (-> schema io/resource slurp edn/read-string))
    (bind-root #'db-properties-file (io/resource properties))
    (let [properties (-> db-properties-file slurp edn/read-string)]
      (assert (or (empty? properties)
                  (apply distinct? (map :property/id properties))))
      (run! validate! properties)
      (bind-root #'db-properties (zipmap (map :property/id properties) properties)))))

(defn -main []
  (let [{:keys [requires
                dock-icon
                mac-os
                title fps width height
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME  "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create  []    (run! create          components))
                          (dispose []    (run! destroy         components))
                          (render  []    (run! render          components))
                          (resize  [w h] (run! #(resize % w h) components)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle title)
                          (.setForegroundFPS fps)
                          (.setWindowedMode width height)))))
