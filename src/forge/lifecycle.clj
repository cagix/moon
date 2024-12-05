(ns forge.lifecycle
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [clojure.string :as str]
            [clojure.vis-ui :as vis]
            [forge.component :refer [defsystem defmethods]]
            [forge.core :refer [batch]]
            [forge.assets :as assets]
            [forge.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn- asset-descriptons [folder]
  (for [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                      [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (utils/recursively-search folder exts))]
    [file class]))

(defsystem create)
(defmethod create :default [_])

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods :app/assets
  (create [[_ folder]]
    (assets/load-all (asset-descriptons folder)))
  (dispose [_]
    (assets/dispose)))

(defmethods :app/vis-ui
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

(defmethods :app/sprite-batch
  (create [_]
    (.bindRoot #'batch (SpriteBatch.)))
  (dispose [_]
    (SpriteBatch/.dispose batch)))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    (awt/set-dock-icon (:dock-icon config))
    (when shared-library-loader/mac?
      (lwjgl/configure {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (lwjgl3/app (proxy [ApplicationAdapter] []
                  (create  []    (run! create          components))
                  (dispose []    (run! dispose         components))
                  (render  []    (run! render          components))
                  (resize  [w h] (run! #(resize % w h) components)))
                (lwjgl3/config (:lwjgl3 config)))))
