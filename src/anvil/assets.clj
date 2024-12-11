(ns anvil.assets
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.files FileHandle)))

(defn- asset-manager* ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- load-assets [^AssetManager manager assets]
  (doseq [[file class-k] assets]
    (.load manager ^String file (case class-k
                                  :sound   com.badlogic.gdx.audio.Sound
                                  :texture com.badlogic.gdx.graphics.Texture)))
  (.finishLoading manager))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn setup [{:keys [folder asset-type-exts]}]
  (def manager (doto (asset-manager*)
                 (load-assets (for [[asset-type exts] asset-type-exts
                                    file (map #(str/replace-first % folder "")
                                              (recursively-search folder exts))]
                                [file asset-type])))))

(defn cleanup []
  (AssetManager/.dispose manager))
