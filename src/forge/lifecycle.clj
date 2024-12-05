(ns forge.lifecycle
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.app.asset-manager]
            [forge.app.cached-map-renderer]
            [forge.app.cursors]
            [forge.app.default-font]
            [forge.app.db]
            [forge.app.gui-viewport]
            [forge.app.shape-drawer]
            [forge.app.screens]
            [forge.app.sprite-batch]
            [forge.app.vis-ui]
            [forge.app.world-viewport]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)
           (java.awt Taskbar Toolkit)))

(declare ^:dynamic *k*)

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (ns-resolve ns-sym (:name (meta system-var)))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))
      (let [system @system-var]
        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))
        (clojure.lang.MultiFn/.addMethod system k (fn call-method [[k & vs] & args]
                                                    (binding [*k* k]
                                                      (apply method-var (into (vec vs) args)))))))))
(defn install-component [component-systems ns-sym k]
  (require ns-sym)
  (add-methods (:required component-systems) ns-sym k)
  (add-methods (:optional component-systems) ns-sym k :optional? true))

(defn- namespace->component-key [prefix ns-str]
  (let [ns-parts (-> ns-str
                     (str/replace prefix "")
                     (str/split #"\."))]
    (keyword (str/join "." (drop-last ns-parts))
             (last ns-parts))))

#_(install-component
 {:optional [#'create
             #'destroy
             #'render
             #'resize]}
 (symbol (component-k->namespace :app/db))
 :app/db)



(defn- component-k->namespace [k]
  (str "forge." (namespace k) "." (name k)))

(comment
 (and (= (namespace->component-key #"^forge." "forge.app/db")
         :effects/projectile)
      (= (namespace->component-key #"^forge." "forge.effects.target.convert")
         :effects.target/convert)))

(defn- install
  ([component-systems ns-sym]
   (install-component component-systems
                      ns-sym
                      (namespace->component-key #"^forge." (str ns-sym))))
  ([component-systems ns-sym k]
   (install-component component-systems ns-sym k)))

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

(defsystem create)
(defmethod create :default [_])

(defsystem destroy)
(defmethod destroy :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods :app/asset-manager
  (create [[_ folder]] (forge.app.asset-manager/create folder))
  (destroy [_]         (forge.app.asset-manager/destroy)))

(defmethods :app/vis-ui
  (create [[_ skin-scale]] (forge.app.vis-ui/create skin-scale))
  (destroy [_]             (forge.app.vis-ui/destroy)))

(defmethods :app/sprite-batch
  (create  [_] (forge.app.sprite-batch/create))
  (destroy [_] (forge.app.sprite-batch/destroy)))

(defmethods :app/shape-drawer
  (create  [_] (forge.app.shape-drawer/create))
  (destroy [_] (forge.app.shape-drawer/destroy)))

(defmethods :app/cursors
  (create [[_ data]] (forge.app.cursors/create data))
  (destroy [_]       (forge.app.cursors/destroy)))

(defmethods :app/gui-viewport
  (create [[_ config]] (forge.app.gui-viewport/create config))
  (resize [_ w h]      (forge.app.gui-viewport/resize w h)))

(defmethods :app/world-viewport
  (create [[_ config]] (forge.app.world-viewport/create config))
  (resize [_ w h]      (forge.app.world-viewport/resize w h)))

(defmethods :app/cached-map-renderer
  (create [_] (forge.app.cached-map-renderer/create)))

(defmethods :app/screens
  (create [[_ config]] (forge.app.screens/create config))
  (destroy [_]         (forge.app.screens/destroy))
  (render [_]          (forge.app.screens/render)))

(defmethods :app/db
  (create [[_ config]] (forge.app.db/create config)))

(defmethods :app/default-font
  (create [[_ font]] (forge.app.default-font/create font))
  (destroy [_]       (forge.app.default-font/destroy)))

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
