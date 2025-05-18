(ns cdq.application.create.assets
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.files :as files]
            [gdl.files.file-handle :as fh]))

(defn- search [{:keys [folder
                       asset-type-extensions]}]
  (for [[asset-type extensions] asset-type-extensions
        file (map #(str/replace-first % folder "")
                  (loop [[file & remaining] (fh/list (files/internal folder))
                         result []]
                    (cond (nil? file)
                          result

                          (fh/directory? file)
                          (recur (concat remaining (fh/list file)) result)

                          (extensions (fh/extension file))
                          (recur remaining (conj result (fh/path file)))

                          :else
                          (recur remaining result))))]
    [file asset-type]))

(defn do! []
  (bind-root #'ctx/assets (assets/create (search (:assets ctx/config)))))
