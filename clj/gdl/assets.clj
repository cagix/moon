(ns gdl.assets
  (:require [clojure.string :as str]
            [clojure.files.file-handle :as fh])
  (:import (com.badlogic.gdx Files)))

(defn search [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (Files/.internal files path)])
       (fh/recursively-search (Files/.internal files folder) extensions)))

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
