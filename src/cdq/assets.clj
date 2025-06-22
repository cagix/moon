(ns cdq.assets
  (:require [gdl.files :as files]
            [gdl.utils.assets :as assets]))

(defn sounds [{:keys [ctx/files]} {:keys [folder extensions]}]
  (map (fn [path]
         [path (files/internal files path)])
       (assets/search (files/internal files folder)
                      extensions)))
(comment

 ; For Uberjar:

 (def sound-paths
   (vec (assets/search (.internal com.badlogic.gdx.Gdx/files "resources/") #{"wav"})))

 (defn get-paths2 [{:keys [ctx/files]} _params]
   (map (fn [path]
          [path (files/internal files path)])
        sound-paths))
 )
