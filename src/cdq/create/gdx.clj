(ns cdq.create.gdx
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

(defn- assets-to-load [files
                       {:keys [folder
                               asset-type-extensions]}]
  (for [[asset-type extensions] asset-type-extensions
        file (map #(str/replace-first % folder "")
                  (recursively-search (files/internal files folder)
                                      extensions))]
    [file asset-type]))

(defn do! [ctx {:keys [assets
                       tile-size
                       ui-viewport
                       world-viewport
                       ]}]
  (let [files (gdx/files)
        world-unit-scale (float (/ tile-size))]
    (assoc ctx
           :ctx/files files
           :ctx/input (gdx/input)
           :ctx/graphics (gdx/graphics)
           :ctx/assets (gdx/asset-manager (assets-to-load files assets))
           :ctx/world-unit-scale world-unit-scale
           :ctx/ui-viewport (gdx/ui-viewport ui-viewport)
           :ctx/world-viewport (gdx/world-viewport world-unit-scale world-viewport)
           )))
