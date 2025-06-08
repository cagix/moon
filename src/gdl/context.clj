(ns gdl.context
  (:require [clojure.gdx.java :as gdx.java]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.string :as str]
            [gdl.create.audio]
            [gdl.create.graphics]
            [gdl.create.input]
            [gdl.create.stage]
            [gdl.utils.disposable :as disposable])
  (:import (com.badlogic.gdx.utils Disposable)))

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

(defn create [config]
  (let [{:keys [clojure.gdx/audio
                clojure.gdx/files
                clojure.gdx/input] :as context} (gdx.java/context)
        graphics-config (update (:graphics config) :textures (partial find-assets files))
        graphics (gdl.create.graphics/create-graphics (:clojure.gdx/graphics context)
                                                      files
                                                      graphics-config)
        stage (gdl.create.stage/create! (:ui config)
                                        graphics
                                        input)]
    {:ctx/input (gdl.create.input/create-input input)
     :ctx/audio (when (:sounds config)
                  (gdl.create.audio/create-audio audio files (find-assets files (:sounds config))))
     :ctx/graphics graphics
     :ctx/stage stage}))
