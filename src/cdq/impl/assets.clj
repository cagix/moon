(ns cdq.impl.assets
  (:require [cdq.files :as files]
            [cdq.gdx.assets :as assets]
            [cdq.gdx.files.file-handle :as fh]
            [clojure.string :as str]))

(defn manager [{:keys [cdq/files]}]
  (assets/create
   (let [folder "resources/"]
     (for [[asset-type extensions] {:sound   #{"wav"}
                                    :texture #{"png" "bmp"}}
           file (map #(str/replace-first % folder "")
                     (loop [[file & remaining] (fh/list (files/internal files folder))
                            result []]
                       (cond (nil? file)
                             result

                             (fh/directory? file)
                             (recur (concat remaining (fh/list file)) result)

                             (extensions (fh/extension file))
                             (recur remaining (conj result (fh/path file)))

                             :else
                             (recur remaining result))))]
       [file asset-type]))))
