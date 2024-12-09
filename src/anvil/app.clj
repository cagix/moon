(ns anvil.app
  (:require [anvil.db :as db]
            [anvil.screen :as screen]
            [clojure.awt :as awt]
            [clojure.component :refer [defsystem] :as component]
            [clojure.gdx.app :as app]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.viewport :as vp :refer [fit-viewport]]
            [clojure.lwjgl :as lwjgl]
            [clojure.string :as str]
            [clojure.utils :refer [defmethods mapvals]]
            [clojure.vis-ui :as vis])
  (:import (forge OrthogonalTiledMapRenderer)))

(defsystem create)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods :db
  (create [[_ config]]
    (db/setup config)))

(defmethods :asset-manager
  (create [[_ folder]]
    (def asset-manager (manager/load-all
                        (for [[asset-type exts] [[:sound   #{"wav"}]
                                                 [:texture #{"png" "bmp"}]]
                              file (map #(str/replace-first % folder "")
                                        (files/recursively-search folder exts))]
                          [file asset-type]))))

  (dispose [_]
    (disposable/dispose asset-manager)))

(defmethods :sprite-batch
  (create [_]
    (def batch (g/sprite-batch)))

  (dispose [_]
    (disposable/dispose batch)))

(let [pixel-texture (atom nil)]
  (defmethods :shape-drawer
    (create [_]
      (reset! pixel-texture (let [pixmap (doto (g/pixmap 1 1)
                                           (.setColor color/white)
                                           (.drawPixel 0 0))
                                  texture (g/texture pixmap)]
                              (disposable/dispose pixmap)
                              texture))
      (def sd (sd/create batch (g/texture-region @pixel-texture 1 0 1 1))))

    (dispose [_]
      (disposable/dispose @pixel-texture))))

(defmethods :default-font
  (create [[_ font]]
    (def default-font (freetype/generate-font font)))

  (dispose [_]
    (disposable/dispose default-font)))

(defmethods :cursors
  (create [[_ data]]
    (def cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                            (let [pixmap (g/pixmap (files/internal (str "cursors/" file ".png")))
                                  cursor (g/cursor pixmap hotspot-x hotspot-y)]
                              (disposable/dispose pixmap)
                              cursor))
                          data)))

  (dispose [_]
    (run! disposable/dispose (vals cursors))))

(defmethods :gui-viewport
  (create [[_ [width height]]]
    (def gui-viewport-width  width)
    (def gui-viewport-height height)
    (def gui-viewport (fit-viewport width height (g/orthographic-camera))))

  (resize [_ w h]
    (vp/update gui-viewport w h :center-camera? true)))

(defmethods :world-viewport
  (create [[_ [width height tile-size]]]
    (def world-unit-scale (float (/ tile-size)))
    (def world-viewport-width  width)
    (def world-viewport-height height)
    (def world-viewport (let [world-width  (* width  world-unit-scale)
                              world-height (* height world-unit-scale)
                              camera (g/orthographic-camera)
                              y-down? false]
                          (.setToOrtho camera y-down? world-width world-height)
                          (fit-viewport world-width world-height camera))))
  (resize [_ w h]
    (vp/update world-viewport w h)))

(defmethods :cached-map-renderer
  (create [_]
    (def cached-map-renderer
      (memoize (fn [tiled-map]
                 (OrthogonalTiledMapRenderer. tiled-map
                                              (float world-unit-scale)
                                              batch))))))

(defmethods :vis-ui
  (create [[_ skin-scale]]
    ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
    ; => actually there is a deeper issue at play
    ; we need to dispose ALL resources which were loaded already ...
    (when (vis/loaded?)
      (vis/dispose))
    (vis/load skin-scale)
    (-> (vis/skin)
        (.getFont "default-font")
        .getData
        .markupEnabled
        (set! true))
    (vis/configure-tooltips {:default-appear-delay-time 0}))

  (dispose [_]
    (vis/dispose)))

(defsystem actors)
(defmethod actors :default [_])

(defmethods :screens/stage
  (screen/enter [[_ {:keys [stage sub-screen]}]]
    (input/set-processor stage)
    (screen/enter sub-screen))

  (screen/exit [[_ {:keys [stage sub-screen]}]]
    (input/set-processor nil)
    (screen/exit sub-screen))

  (screen/render [[_ {:keys [stage sub-screen]}]]
    (stage/act stage)
    (screen/render sub-screen)
    (stage/draw stage))

  (screen/dispose [[_ {:keys [stage sub-screen]}]]
    (disposable/dispose stage)
    (screen/dispose sub-screen)))

(defmethods :screens
  (create [[_ {:keys [screens first-k]}]]
    (screen/setup (into {}
                        (for [k screens]
                          [k [:screens/stage {:stage (stage/create gui-viewport
                                                                   batch
                                                                   (actors [k]))
                                              :sub-screen [k]}]]))
                  first-k))

  (dispose [_]
    (screen/dispose-all))

  (render [_]
    (g/clear-screen color/black)
    (screen/render-current)))

(defn start [{:keys [dock-icon components lwjgl3]}]
  (awt/set-dock-icon dock-icon)
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (reify lwjgl3/Listener
                (create  [_]     (run! create          components))
                (dispose [_]     (run! dispose         components))
                (render  [_]     (run! render          components))
                (resize  [_ w h] (run! #(resize % w h) components)))
              (lwjgl3/config lwjgl3)))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))
