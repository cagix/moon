(ns gdl.context.assets
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.file-handle :as fh]
            [clojure.string :as str]))

(defn- load-all [manager assets]
  (doseq [[file asset-type] assets]
    (assets/load manager file asset-type))
  (assets/finish-loading manager))

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

(def ^:private asset-type-exts {:sound   #{"wav"}
                                :texture #{"png" "bmp"}})

(defn create [[_ folder] context]
  (doto (gdx/asset-manager)
    (load-all (for [[asset-type exts] asset-type-exts
                    file (map #(str/replace-first % folder "")
                              (recursively-search (gdx/internal-file context folder)
                                                  exts))]
                [file asset-type]))))

(defn dispose [[_ asset-manager]]
  (gdx/dispose asset-manager))
