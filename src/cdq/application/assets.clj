(ns cdq.application.assets
  (:require [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.files :as files]
            [gdl.files.file-handle :as fh]))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list (files/internal folder))
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn create [{:keys [folder
                      asset-type-extensions]}]
  (assets/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search folder extensions))]
     [file asset-type])))

(defn all-textures [assets]
  (assets/all-of-type assets :texture))

(defn all-sounds [assets]
  (assets/all-of-type assets :sound))
