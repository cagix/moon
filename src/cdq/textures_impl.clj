(ns cdq.textures-impl
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.string :as str]))

(defn- search-files [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (files/internal files path)])
       (file-handle/recursively-search (files/internal files folder) extensions)))

(defn create [files]
  (into {} (for [[path file-handle] (search-files files
                                                  {:folder "resources/"
                                                   :extensions #{"png" "bmp"}})]
             [path (texture/from-file file-handle)])))
