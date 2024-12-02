(ns forge.start
  (:require [forge.screen :as screen]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture OrthographicCamera Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils Disposable SharedLibraryLoader)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)
           (forge OrthogonalTiledMapRenderer)))

(defn- background-image []
  (ui/image->widget (->image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(def tile-size 48)

(def ^:private config "app.edn")

(defn- set-dock-icon [image-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io-resource image-resource))))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

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
            file (map #(str-replace-first % folder "")
                      (recursively-search (internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(declare ^:private ^Texture shape-drawer-texture)

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(defrecord StageScreen [^Stage stage sub-screen]
  screen/Screen
  (enter [_]
    (set-input-processor stage)
    (screen/enter sub-screen))

  (exit [_]
    (set-input-processor nil)
    (screen/exit sub-screen))

  (render [_]
    (.act stage)
    (screen/render sub-screen)
    (.draw stage))

  (destroy [_]
    (dispose stage)
    (screen/destroy sub-screen)))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [Stage clojure.lang.ILookup] [gui-viewport batch]
                (valAt
                  ([id]
                   (ui/find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (ui/find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defn- create-screens [screen-ks]
  (mapvals stage-screen
           (mapvals
            (fn [ns-sym]
              (require ns-sym)
              (let [create-fn (ns-resolve ns-sym 'create)]
                (create-fn (background-image))))
            screen-ks)))

(defn -main []
  (let [{:keys [dock-icon
                lwjgl3
                db/schema
                db/properties
                assets
                cursors
                ui
                requires
                screen-ks
                first-screen-k]} (-> config io-resource slurp edn-read-string)]
    (run! require requires)
    (bind-root #'db-schemas (-> schema io-resource slurp edn-read-string))
    (bind-root #'db-properties-file (io-resource properties))
    (let [properties (-> db-properties-file slurp edn-read-string)]
      (assert (or (empty? properties)
                  (apply distinct? (map :property/id properties))))
      (run! validate! properties)
      (bind-root #'db-properties (zipmap (map :property/id properties) properties)))
    (set-dock-icon dock-icon)
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application.
     (proxy [ApplicationAdapter] []
       (create []
         (bind-root #'asset-manager (load-assets assets))
         (bind-root #'batch (SpriteBatch.))
         (bind-root #'shape-drawer-texture (white-pixel-texture))
         (bind-root #'shape-drawer (ShapeDrawer. batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
         (bind-root #'default-font (truetype-font
                                    {:file (internal-file "fonts/exocet/films.EXL_____.ttf")
                                     :size 16
                                     :quality-scaling 2}))
         (bind-root #'world-unit-scale (float (/ tile-size)))
         (bind-root #'world-viewport (let [world-width  (* world-viewport-width  world-unit-scale)
                                           world-height (* world-viewport-height world-unit-scale)
                                           camera (OrthographicCamera.)
                                           y-down? false]
                                       (.setToOrtho camera y-down? world-width world-height)
                                       (FitViewport. world-width world-height camera)))
         (bind-root #'cached-map-renderer (memoize
                                           (fn [tiled-map]
                                             (OrthogonalTiledMapRenderer. tiled-map
                                                                          (float world-unit-scale)
                                                                          batch))))
         (bind-root #'gui-viewport (FitViewport. gui-viewport-width
                                                 gui-viewport-height
                                                 (OrthographicCamera.)))
         (bind-root #'cursors (mapvals gdx-cursor cursors))
         (ui/init ui)
         (bind-root #'screens (create-screens screen-ks))
         (change-screen first-screen-k))

       (dispose []
         (dispose asset-manager)
         (dispose batch)
         (dispose shape-drawer-texture)
         (dispose default-font)
         (run! dispose (vals cursors))
         (ui/destroy)
         (run! screen/destroy (vals screens)))

       (render []
         (clear-screen black)
         (screen/render (current-screen)))

       (resize [w h]
         (.update gui-viewport   w h true)
         (.update world-viewport w h)))
     (lwjgl3-config lwjgl3))))
