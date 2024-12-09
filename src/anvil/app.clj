(ns anvil.app
  (:require [clojure.awt :as awt]
            [clojure.component :refer [defsystem]]
            [clojure.gdx.app :as app]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl :as lwjgl]))

(defsystem create)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defn start [{:keys [dock-icon lwjgl3 app]}]
  (awt/set-dock-icon dock-icon)
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (reify lwjgl3/Listener
                (create  [_]     (run! create          app))
                (dispose [_]     (run! dispose         app))
                (render  [_]     (run! render          app))
                (resize  [_ w h] (run! #(resize % w h) app)))
              (lwjgl3/config lwjgl3)))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))

(declare asset-manager
         batch
         sd
         default-font
         cursors
         gui-viewport-width
         gui-viewport-height
         gui-viewport
         world-unit-scale
         world-viewport-width
         world-viewport-height
         world-viewport
         cached-map-renderer)
