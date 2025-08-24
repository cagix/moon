(ns cdq.assets
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Files)
           (com.badlogic.gdx.files FileHandle)))

(defn- recursively-search [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn search [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (Files/.internal files path)])
       (recursively-search (Files/.internal files folder) extensions)))

(comment

 ; For Uberjar:

 (def sound-paths
   (vec (assets/search (.internal com.badlogic.gdx.Gdx/files "resources/") #{"wav"})))

 (def texture-paths
   (vec (assets/search (.internal com.badlogic.gdx.Gdx/files "resources/")
                       #{"png" "bmp"})))

 (defn get-paths2 [{:keys [ctx/files]} _params]
   (map (fn [path]
          [path (Files/.internal files path)])
        sound-paths))
 )
