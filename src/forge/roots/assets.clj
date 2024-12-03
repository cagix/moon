(ns forge.roots.assets
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

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

(defn load [folder]
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [^String path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[class exts] [[Sound   #{"wav"}]
                          [Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (recursively-search (.internal Gdx/files folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))
