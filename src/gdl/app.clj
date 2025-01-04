(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.file-handle :as fh]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.gdx.vis-ui :as vis-ui]
            [clojure.string :as str]
            [gdl.utils :refer [defsystem defcomponent mapvals]]
            [gdl.db :as db]
            [gdl.ui :as ui])
  (:import (com.kotcrab.vis.ui.widget Tooltip)
           (forge OrthogonalTiledMapRenderer)))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem resize)
(defmethod resize :default [_ width height])

(defn- safe-create-into [context components]
  (reduce (fn [context [k v]]
            (assert (not (contains? context k)))
            (assoc context k (create [k v] context)))
          context
          components))

(defn- reduce-transact [context transactions]
  (reduce (fn [context f]
            (f context))
          context
          transactions))

(def state (atom nil))

(comment
 (clojure.pprint/pprint (sort (keys @state)))
 )

(defn start
  "A transaction is a `(fn [context] context)`, which can emit also side-effects or return a new context."
  [{:keys [config context transactions]}]
  (lwjgl/start config
               (reify lwjgl/Application
                 (create [_]
                   (reset! state (safe-create-into (gdx/context) context)))

                 (dispose [_]
                   (run! dispose @state))

                 (render [_]
                   (swap! state reduce-transact transactions))

                 (resize [_ width height]
                   (run! #(resize % width height) @state)))))

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

(defn- sd-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color gdx/white)
                 (gdx/draw-pixel 0 0))
        texture (gdx/texture pixmap)]
    (gdx/dispose pixmap)
    texture))

(defcomponent :gdl.context/assets
  (create [[_ folder] context]
    (doto (gdx/asset-manager)
      (load-all (for [[asset-type exts] {:sound   #{"wav"}
                                         :texture #{"png" "bmp"}}
                      file (map #(str/replace-first % folder "")
                                (recursively-search (gdx/internal-file context folder)
                                                    exts))]
                  [file asset-type]))))

  (dispose [[_ asset-manager]]
    (gdx/dispose asset-manager)))

(defcomponent :gdl.context/batch
  (create [_ _context]
    (gdx/sprite-batch))

  (dispose [[_ batch]]
    (gdx/dispose batch)))

(defcomponent :gdl.context/cursors
  (create [[_ cursors] c]
    (mapvals (fn [[file [hotspot-x hotspot-y]]]
               (let [pixmap (gdx/pixmap (gdx/internal-file c (str "cursors/" file ".png")))
                     cursor (gdx/cursor c pixmap hotspot-x hotspot-y)]
                 (gdx/dispose pixmap)
                 cursor))
             cursors))

  (dispose [[_ cursors]]
    (run! gdx/dispose (vals cursors))))

(defcomponent :gdl.context/db
  (create [[_ config] _context]
    (db/create config)))

(defcomponent :gdl.context/default-font
  (create [[_ config] context]
    (freetype/generate-font (update config :file #(gdx/internal-file context %))))

  (dispose [[_ font]]
    (gdx/dispose font)))

(defcomponent :gdl.context/shape-drawer
  (create [_ {:keys [gdl.context/batch]}]
    (assert batch)
    (sd/create batch (gdx/texture-region (sd-texture) 1 0 1 1)))

  (dispose [[_ sd]]
    #_(gdx/dispose sd))
  ; TODO this will break ... proxy with extra-data -> get texture through sd ...
  ; => shape-drawer-texture as separate component?!
  ; that would work
  )

(defcomponent :gdl.context/stage
  (create [_ {:keys [gdl.context/viewport
                     gdl.context/batch] :as c}]
    (let [stage (ui/stage viewport batch nil)]
      (gdx/set-input-processor c stage)
      stage))

  (dispose [[_ stage]]
    (gdx/dispose stage)))

(defcomponent :gdl.context/tiled-map-renderer
  (create [_ {:keys [gdl.context/world-unit-scale
                     gdl.context/batch]}]
    (assert world-unit-scale)
    (assert batch)
    (memoize (fn [tiled-map]
               (OrthogonalTiledMapRenderer. tiled-map
                                            (float world-unit-scale)
                                            batch)))))

(defcomponent :gdl.context/ui
  (create [[_ skin-scale] _c]
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

  (dispose [_]
    (vis-ui/dispose)))

(defcomponent :gdl.context/viewport
  (create [[_ {:keys [width height]}] _c]
    (gdx/fit-viewport width height (gdx/orthographic-camera)))

  (resize [[_ viewport] w h]
    (gdx/resize viewport w h :center-camera? true)))

(defcomponent :gdl.context/world-unit-scale
  (create [[_ tile-size] _c]
    (float (/ tile-size))))

(defcomponent :gdl.context/world-viewport
  (create [[_ {:keys [width height]}] {:keys [gdl.context/world-unit-scale]}]
    (assert world-unit-scale)
    (let [camera (gdx/orthographic-camera)
          world-width  (* width  world-unit-scale)
          world-height (* height world-unit-scale)]
      (camera/set-to-ortho camera world-width world-height :y-down? false)
      (gdx/fit-viewport world-width world-height camera)))

  (resize [[_ viewport] w h]
    (gdx/resize viewport w h :center-camera? false)))
