(ns gdl.app.create
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.file-handle :as fh]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.vis-ui :as vis-ui]
            [clojure.string :as str]
            [gdl.db :as db]
            [gdl.ui :as ui]
            [gdl.utils :refer [mapvals safe-merge]])
  (:import (com.kotcrab.vis.ui.widget Tooltip)
           (gdl OrthogonalTiledMapRenderer)))

(defn- load-all [manager assets]
  (doseq [[file asset-type] assets]
    (assets/load manager file asset-type))
  (assets/finish-loading manager))

(defn- recursively-search [folder extensions]
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

(defn- load-assets [context folder]
  (doto (gdx/asset-manager)
    (load-all (for [[asset-type exts] {:sound   #{"wav"}
                                       :texture #{"png" "bmp"}}
                    file (map #(str/replace-first % folder "")
                              (recursively-search (gdx/internal-file context folder)
                                                  exts))]
                [file asset-type]))))

(defn- create-cursors [context cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (gdx/pixmap (gdx/internal-file context (str "cursors/" file ".png")))
                   cursor (gdx/cursor context pixmap hotspot-x hotspot-y)]
               (gdx/dispose pixmap)
               cursor))
           cursors))

(defn- white-pixel-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color gdx/white)
                 (gdx/draw-pixel 0 0))
        texture (gdx/texture pixmap)]
    (gdx/dispose pixmap)
    texture))

(defn- cached-tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- load-vis-ui! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (vis-ui/loaded?)
    (vis-ui/dispose))
  (vis-ui/load skin-scale)
  (-> (vis-ui/skin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (println "World-viewport: " width ", " height ", world-unit-scale " world-unit-scale)
  (assert world-unit-scale)
  (let [camera (gdx/orthographic-camera)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (gdx/fit-viewport world-width world-height camera)))

(defn context [context config]
  (load-vis-ui! (:vis-ui config))
  (let [batch (gdx/sprite-batch)
        sd-texture (white-pixel-texture)
        ui-viewport (gdx/fit-viewport (:width  (:ui-viewport config))
                                      (:height (:ui-viewport config))
                                      (gdx/orthographic-camera))
        world-unit-scale (float (/ (:tile-size config)))
        stage (ui/stage ui-viewport batch nil)]
    (gdx/set-input-processor context stage)
    (safe-merge context
                {:gdl.context/assets (load-assets context (:assets config))
                 :gdl.context/batch batch
                 :gdl.context/cursors (create-cursors context (:cursors config))
                 :gdl.context/db (db/create (:db config))
                 :gdl.context/default-font (freetype/generate-font (update (:default-font config) :file #(gdx/internal-file context %)))
                 :gdl.context/shape-drawer (sd/create batch (gdx/texture-region sd-texture 1 0 1 1))
                 :gdl.context/sd-texture sd-texture
                 :gdl.context/stage stage
                 :gdl.context/viewport ui-viewport
                 :gdl.context/world-viewport (world-viewport (:world-viewport config) world-unit-scale)
                 :gdl.context/world-unit-scale world-unit-scale
                 :gdl.context/tiled-map-renderer (cached-tiled-map-renderer batch world-unit-scale)
                 :gdl.context/elapsed-time 0})))
