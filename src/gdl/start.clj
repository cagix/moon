(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration]
            [clojure.string :as str]
            [gdl.create.audio]
            [gdl.create.graphics]
            [gdl.create.input]
            [gdl.create.stage]
            [gdl.utils.disposable :as disposable])
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.utils Disposable)))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (file-handle/list folder)
         result []]
    (cond (nil? file)
          result

          (file-handle/directory? file)
          (recur (concat remaining (file-handle/list file)) result)

          (extensions (file-handle/extension file))
          (recur remaining (conj result (file-handle/path file)))

          :else
          (recur remaining result))))

(extend-type Disposable
  disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- find-assets [files {:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (recursively-search (files/internal files folder)
                           extensions)))

(defn- create-context [config]
  (let [graphics-config (update (:graphics config) :textures (partial find-assets (gdx/files)))
        graphics (gdl.create.graphics/create-graphics (gdx/graphics)
                                                      (gdx/files)
                                                      graphics-config)
        stage (gdl.create.stage/create! (:ui config)
                                        graphics
                                        (gdx/input))]
    {:ctx/input (gdl.create.input/create-input (gdx/input))
     :ctx/audio (when (:sounds config)
                  (gdl.create.audio/create-audio (gdx/audio)
                                                 (gdx/files)
                                                 (find-assets (gdx/files)
                                                              (:sounds config))))
     :ctx/graphics graphics
     :ctx/stage stage}))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))

(defn- create-listener [config]
  (let [req-resolve-call (fn [k & params]
                           (when-let [f (k config)]
                             (apply (requiring-resolve f) params)))]
    (proxy [ApplicationListener] []
      (create  []
        ((requiring-resolve (:clojure.gdx.lwjgl/create! config))
         (create-context (:gdl.application/context config))
         config))
      (dispose []             (req-resolve-call :clojure.gdx.lwjgl/dispose!))
      (render  []             (req-resolve-call :clojure.gdx.lwjgl/render!))
      (resize  [width height] (req-resolve-call :clojure.gdx.lwjgl/resize! width height))
      (pause   []             (req-resolve-call :clojure.gdx.lwjgl/pause!))
      (resume  []             (req-resolve-call :clojure.gdx.lwjgl/resume!)))))

(defn- operating-system []
  (get os/mapping (shared-library-loader/os)))

(defn- read-config [path]
  (-> path
      io/resource
      slurp
      edn/read-string))

(defn -main [config-path]
  (let [config (read-config config-path)]
    (when (= (operating-system) :os/mac-osx)
      (set-mac-os-config! (:mac-os config)))
    (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                        (create-listener config))))
