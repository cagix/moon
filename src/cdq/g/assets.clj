(ns cdq.g.assets
  (:require [cdq.assets :as assets]
            [clojure.gdx :as gdx]
            [clojure.gdx.asset-manager :as asset-manager]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.string :as str]))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (file-handle/list folder)
         result []]
    (cond (nil? file)
          result

          (file-handle/directory? file)
          (recur (concat remaining (file-handle/list file)) result)

          (extensions (file-handle/extension file))
          (recur remaining (conj result (file-handle/path file)))

          :else
          (recur remaining result))))

(defn- search [folder]
  (for [[asset-type extensions] {com.badlogic.gdx.audio.Sound      #{"wav"}
                                 com.badlogic.gdx.graphics.Texture #{"png" "bmp"}}
        file (map #(str/replace-first % folder "")
                  (recursively-search (gdx/internal folder) extensions))]
    [file asset-type]))

(defn create [folder]
  (let [this (asset-manager/create (search folder))]
    (reify clojure.lang.IFn
      (invoke [_ path]
        (asset-manager/get this path))
      assets/Assets
      (all-of-type [_ asset-type]
        (asset-manager/all-of-type this asset-type))
      com.badlogic.gdx.utils.Disposable
      (dispose [_]
        (com.badlogic.gdx.utils.Disposable/.dispose this)))))
