(ns clojure.create.assets
  (:require [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.gdx :as gdx]
            [clojure.string :as str]))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn- get-assets-to-load [files
                           {:keys [folder
                                   asset-type-extensions]}]
  (for [[asset-type extensions] asset-type-extensions
        file (map #(str/replace-first % folder "")
                  (recursively-search (files/internal files folder)
                                      extensions))]
    [file asset-type]))

(defn do! [{:keys [ctx/config
                   ctx/files]
            :as ctx}]
  (assoc ctx :ctx/assets (gdx/asset-manager (get-assets-to-load files (:assets config)))))
