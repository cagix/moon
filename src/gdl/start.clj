(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.gdx.input :as input]
            [clojure.gdx.input.buttons :as input.buttons]
            [clojure.gdx.input.keys :as input.keys]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration]
            [clojure.string :as str]
            [gdl.audio]
            [gdl.create.graphics]
            [gdl.create.stage]
            [gdl.input]
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

(defn- create-audio [audio files sounds-to-load]
  ;(println "create-audio. (count sounds-to-load): " (count sounds-to-load))
  (let [sounds (into {}
                     (for [file sounds-to-load]
                       [file (audio/sound audio (files/internal files file))]))]
    (reify
      disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! disposable/dispose! (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (sound/play! (get sounds path))))))

(defn- create-input [this]
  (reify gdl.input/Input
    (button-just-pressed? [_ button]
      (input/button-just-pressed? this (input.buttons/->from-k button)))

    (key-pressed? [_ key]
      (input/key-pressed? this (input.keys/->from-k key)))

    (key-just-pressed? [_ key]
      (input/key-just-pressed? this (input.keys/->from-k key)))

    (mouse-position [_]
      [(input/x this)
       (input/y this)])))

(defn- create-context [{:keys [gdx/audio
                               gdx/files
                               gdx/input]
                        :as context}
                       {:keys [sounds ui]
                        :as config}]
  (let [graphics-config (update (:graphics config) :textures (partial find-assets files))
        graphics (gdl.create.graphics/create-graphics (:gdx/graphics context)
                                                      (:gdx/files context)
                                                      graphics-config)
        stage (gdl.create.stage/create! (:ui config)
                                        graphics
                                        input)]
    {:ctx/input (create-input input)
     :ctx/audio (when sounds (create-audio audio files (find-assets files sounds)))
     :ctx/graphics graphics
     :ctx/stage stage}))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))

(defn -main [app-edn-path]
  (let [config (-> app-edn-path
                   io/resource
                   slurp
                   edn/read-string)
        req-resolve-call (fn [k & params]
                           (when-let [f (k config)]
                             (apply (requiring-resolve f) params)))]
    (when (= (get os/mapping (shared-library-loader/os)) :os/mac-osx)
      (set-mac-os-config! (:mac-os config)))
    (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                        (proxy [ApplicationListener] []
                          (create  []
                            ((requiring-resolve (:clojure.gdx.lwjgl/create! config))
                             (create-context {:gdx/app      (gdx/app)
                                              :gdx/audio    (gdx/audio)
                                              :gdx/files    (gdx/files)
                                              :gdx/graphics (gdx/graphics)
                                              :gdx/input    (gdx/input)}
                                             (:gdl.application/context config))
                             config))
                          (dispose []             (req-resolve-call :clojure.gdx.lwjgl/dispose!))
                          (render  []             (req-resolve-call :clojure.gdx.lwjgl/render!))
                          (resize  [width height] (req-resolve-call :clojure.gdx.lwjgl/resize! width height))
                          (pause   []             (req-resolve-call :clojure.gdx.lwjgl/pause!))
                          (resume  []             (req-resolve-call :clojure.gdx.lwjgl/resume!))))))
