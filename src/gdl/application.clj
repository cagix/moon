(ns gdl.application
  (:require cdq.utils
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.gdx.java :as gdx.java]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl.system.configuration]
            [clojure.java.awt.taskbar]
            [clojure.string :as str]
            [gdl.audio]
            [gdl.create.graphics]
            [gdl.create.input]
            [gdl.create.stage]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.utils Disposable)))

(defn- create-audio [audio files sounds-to-load]
  ;(println "create-audio. (count sounds-to-load): " (count sounds-to-load))
  (let [sounds (into {}
                     (for [file sounds-to-load]
                       [file (audio/sound audio (files/internal files file))]))]
    (reify
      gdl.utils.disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! clojure.gdx.utils.disposable/dispose! (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (sound/play! (get sounds path))))))

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
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- find-assets [files {:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (recursively-search (files/internal files folder)
                           extensions)))

(defn- set-mac-settings! [{:keys [glfw-async? dock-icon]}]
  (when glfw-async?
    (clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (clojure.java.awt.taskbar/set-icon! dock-icon)))

(defn- create-context [main-config]
  (let [config (::context main-config)
        {:keys [clojure.gdx/audio
                clojure.gdx/files
                clojure.gdx/input] :as context} (gdx.java/context)
        graphics-config (update (:graphics config) :textures (partial find-assets files))
        graphics (gdl.create.graphics/create-graphics (:clojure.gdx/graphics context)
                                                      files
                                                      graphics-config)
        stage (gdl.create.stage/create! (:ui config)
                                        graphics
                                        input)]
    {:ctx/config main-config
     :ctx/input (gdl.create.input/create-input input)
     :ctx/audio (when (:sounds config)
                  (create-audio audio files (find-assets files (:sounds config))))
     :ctx/graphics graphics
     :ctx/stage stage}))

(defn -main [config-path]
  (let [{:keys [mac-os-settings lwjgl-app-config listener] :as config} (cdq.utils/load-edn-config config-path)]
    (when (= (shared-library-loader/os) :os/mac-osx)
            (set-mac-settings! mac-os-settings))
    (lwjgl/application lwjgl-app-config
                       (let [{:keys [create dispose render resize pause resume]} listener]
                         (proxy [ApplicationListener] []
                           (create  []              (when-let [[f params] create] (f (create-context config) params)))
                           (dispose []              (when dispose (dispose)))
                           (render  []              (when-let [[f params] render] (f params)))
                           (resize  [width height]  (when resize  (resize width height)))
                           (pause   []              (when pause   (pause)))
                           (resume  []              (when resume  (resume))))))))
