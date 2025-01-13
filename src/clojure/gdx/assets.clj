(ns clojure.gdx.assets
  (:require [clojure.string :as str])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(defn manager
  ([_context _config]
   (manager
    (let [folder "resources/"]
      (for [[asset-type extensions] {Sound   #{"wav"}
                                     Texture #{"png" "bmp"}}
            file (map #(str/replace-first % folder "")
                      (loop [[file & remaining] (.list (.internal Gdx/files folder))
                             result []]
                        (cond (nil? file)
                              result

                              (.isDirectory file)
                              (recur (concat remaining (.list file)) result)

                              (extensions (.extension file))
                              (recur remaining (conj result (.path file)))

                              :else
                              (recur remaining result))))]
        [file asset-type]))))
  ([assets]
   (let [manager (proxy [AssetManager clojure.lang.IFn] []
                   (invoke [^String path]
                     (let [^AssetManager this this]
                       (if (.contains this path)
                         (.get this path)
                         (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))]
     (doseq [[file asset-type] assets]
       (.load manager ^String file asset-type))
     (.finishLoading manager)
     manager)))
