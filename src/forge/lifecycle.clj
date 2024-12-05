(ns forge.lifecycle
  (:require [clojure.component :refer [defsystem defmethods]]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [clojure.string :as str]
            [forge.assets :as assets])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.files FileHandle)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)))

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

(defn- asset-manager ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- load-assets [folder]
  (let [manager (asset-manager)]
    (doseq [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                          [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (recursively-search (.internal Gdx/files folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

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
    (.bindRoot #'assets/get (load-assets folder)))
  (dispose [_]
    (.dispose assets/get)))

(defmethods :app/vis-ui
  (create [[_ skin-scale]]
    (check-cleanup-visui!)
    (VisUI/load (case skin-scale
                  :skin-scale/x1 VisUI$SkinScale/X1
                  :skin-scale/x2 VisUI$SkinScale/X2))
    (font-enable-markup!)
    (set-tooltip-config!))
  (dispose [_]
    (VisUI/dispose)))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn"
                                            io/resource
                                            slurp
                                            edn/read-string)]
    (run! require (:requires config))
    (awt/set-dock-icon (:dock-icon config))
    (when shared-library-loader/mac?
      (lwjgl/configure {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (lwjgl3/app (proxy [com.badlogic.gdx.ApplicationAdapter] []
                  (create  []    (run! create          components))
                  (dispose []    (run! dispose         components))
                  (render  []    (run! render          components))
                  (resize  [w h] (run! #(resize % w h) components)))
                (lwjgl3/config (:lwjgl3 config)))))
