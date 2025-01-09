(ns gdl.app
  (:require [clojure.edn :as edn]
            [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.gdx]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [clojure.graphics :as graphics]
            [clojure.input :as input]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.utils :refer [mapvals]]
            [gdl.assets :as assets]
            [cdq.db :as db]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (gdl OrthogonalTiledMapRenderer)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (assert world-unit-scale)
  (let [camera (orthographic-camera/create)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (fit-viewport/create world-width world-height camera)))

(defn- cached-tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. file)
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- create-cursors [{:keys [clojure.gdx/files
                               clojure.gdx/graphics]} cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (pixmap/create (files/internal files (str "cursors/" file ".png")))
                   cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
               (.dispose pixmap)
               cursor))
           cursors))

(defn- create-graphics [{:keys [clojure.gdx/files] :as context} config]
  (let [batch (SpriteBatch.)
        sd-texture (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                                  (pixmap/set-color color/white)
                                  (pixmap/draw-pixel 0 0))
                         texture (texture/create pixmap)]
                     (.dispose pixmap)
                     texture)
        world-unit-scale (float (/ (:tile-size config)))
        ]
    {:batch batch
     :sd (sd/create batch (texture-region/create sd-texture 1 0 1 1))
     :sd-texture sd-texture
     :cursors (create-cursors context (:cursors config))
     :default-font (generate-font (update (:default-font config) :file #(files/internal files %)))
     :world-unit-scale world-unit-scale
     :tiled-map-renderer (cached-tiled-map-renderer batch world-unit-scale)
     :ui-viewport (fit-viewport/create (:width  (:ui-viewport config))
                                       (:height (:ui-viewport config))
                                       (orthographic-camera/create))
     :world-viewport (world-viewport (:world-viewport config) world-unit-scale)
     }))

(defn- search-by-extensions [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn- search-assets [files folder]
  (for [[asset-type exts] {:sound   #{"wav"}
                           :texture #{"png" "bmp"}}
        file (map #(str/replace-first % folder "")
                  (search-by-extensions (files/internal files folder)
                                        exts))]
    [file asset-type]))

; :gdl.app/gdx and then non-ns keywords
; :gdl.app/graphics and then also
; :gdl.app/stage ...

(defn gdl-context [config]
  (let [gdx {:clojure.gdx/app      Gdx/app
             :clojure.gdx/files    Gdx/files
             :clojure.gdx/graphics Gdx/graphics
             :clojure.gdx/input    Gdx/input}
        g (create-graphics gdx (:graphics config))
        batch (:batch g)
        shape-drawer (:sd g)
        sd-texture (:sd-texture g)
        cursors (:cursors g)
        default-font (:default-font g)
        tiled-map-renderer (:tiled-map-renderer g)
        world-unit-scale (:world-unit-scale g)
        world-viewport (:world-viewport g)
        ui-viewport (:ui-viewport g)

        _ (ui/load! (:ui config))
        stage (ui/stage ui-viewport batch nil)
        _ (input/set-processor (:clojure.gdx/input gdx) stage)

        ]
    (merge gdx
           {:gdl.context/assets (assets/create (search-assets (:clojure.gdx/files gdx)
                                                              (:assets config)))
            :gdl.context/batch batch
            :gdl.context/cursors cursors
            :gdl.context/default-font default-font
            :gdl.context/shape-drawer shape-drawer
            :gdl.context/sd-texture sd-texture
            :gdl.context/db (db/create (:db config))
            :gdl.context/stage stage
            :gdl.context/viewport ui-viewport
            :gdl.context/world-viewport world-viewport
            :gdl.context/world-unit-scale world-unit-scale
            :gdl.context/tiled-map-renderer tiled-map-renderer})))

(def state (atom nil))

(defn post-runnable [f]
  (.postRunnable (:clojure.gdx/app @state) #(f @state)))

(defn start [config-path create dispose render]
  (let [config (-> config-path
                   io/resource
                   slurp
                   edn/read-string)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource (:icon config))))
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      #_(.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state
                                    (create (gdl-context config)
                                            (:context config))))

                          (dispose []
                            (dispose @state))

                          (render []
                            (swap! state render))

                          (resize [width height]
                            (viewport/resize (:gdl.context/viewport       @state) width height :center-camera? true)
                            (viewport/resize (:gdl.context/world-viewport @state) width height :center-camera? false)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:window-width config)
                                            (:window-height config))
                          (.setForegroundFPS (:fps config))))))
