(ns gdl.assets
  (:require [clojure.string :as str]
            [gdl.files :as files]
            [gdl.files.file-handle :as fh]))

(defn search [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (files/internal files path)])
       (fh/recursively-search (files/internal files folder) extensions)))

(comment

 ; For Uberjar:

 (def sound-paths
   (vec (assets/search (.internal com.badlogic.gdx.Gdx/files "resources/") #{"wav"})))

 (def texture-paths
   (vec (assets/search (.internal com.badlogic.gdx.Gdx/files "resources/")
                       #{"png" "bmp"})))

 (defn get-paths2 [{:keys [ctx/files]} _params]
   (map (fn [path]
          [path (files/internal files path)])
        sound-paths))
 )
