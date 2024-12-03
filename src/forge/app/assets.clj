(ns ^:no-doc forge.app.assets
  (:require [clojure.string :as str]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.files FileHandle)))

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
  (proxy [AssetManager clojure.lang.ILookup] []
    (valAt [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- load-assets [folder]
  (let [manager (asset-manager)]
    (doseq [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                          [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (recursively-search (internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defmethods :app/assets
  (app-create [[_ folder]]
    (bind-root #'assets (load-assets folder)))
  (app-dispose [_]
    (.dispose assets)))
