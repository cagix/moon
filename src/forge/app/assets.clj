(ns forge.app.assets
  (:require [clojure.gdx :as gdx]
            [clojure.string :as str]
            [forge.system :as system :refer [defmethods bind-root]])
  (:import (com.badlogic.gdx.assets AssetManager)
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

(defn- asset-manager [folder]
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [^String path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[class exts] [[Sound   #{"wav"}]
                          [Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (recursively-search (gdx/internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defmethods :app/assets
  (system/create [[_ folder]]
    (bind-root #'system/assets (asset-manager folder)))
  (system/dispose [_]
    (.dispose system/assets)))
