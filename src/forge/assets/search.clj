(ns forge.assets.search
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files.handle :as fh]
            [clojure.string :as str]))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list (gdx/internal-file folder))
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn search-assets
  "Returns a collection of `[file-path class]` after recursively searching `folder` and matching file extensions with class as of `asset-description`."
  [folder asset-description]
  (for [[class exts] asset-description
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))
